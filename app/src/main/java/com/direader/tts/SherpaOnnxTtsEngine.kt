package com.direader.tts

import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 基于 Sherpa-ONNX 的离线语音合成真实引擎。
 * 驱动底层 C++ 库在 Android CPU 上进行 ONNX 推理。
 */
class SherpaOnnxTtsEngine : TtsEngine {
    private var tts: OfflineTts? = null
    private var _isReady = false

    override val isReady: Boolean
        get() = _isReady

    override val availableVoices: List<VoiceInfo> = listOf(
        VoiceInfo("zh_CN-huayan-medium", "华言女声 (Piper)", "zh-CN")
    )

    override suspend fun initialize(modelDir: String) = withContext(Dispatchers.IO) {
        val publicPiperDir = File(android.os.Environment.getExternalStorageDirectory(), "DiReader/models/piper")
        val publicModelsDir = File(android.os.Environment.getExternalStorageDirectory(), "DiReader/models")
        val context = com.direader.DiReaderApp.instance
        val privatePiperDir = File(context.getExternalFilesDir(null), "models/piper")
        val privateModelsDir = File(context.getExternalFilesDir(null), "models")
        val inputDir = File(modelDir)

        val candidates = listOf(publicPiperDir, publicModelsDir, privatePiperDir, privateModelsDir, inputDir)
        val targetDir = candidates.firstOrNull { dir ->
            dir.exists() && dir.walkTopDown().maxDepth(3).any { it.isFile && it.extension.equals("onnx", ignoreCase = true) }
        }

        if (targetDir == null) {
            _isReady = false
            return@withContext
        }

        // 递归深层查找 .onnx 模型文件、tokens.txt 及 espeak-ng-data 目录
        val modelFile = targetDir.walkTopDown().maxDepth(3).firstOrNull { it.isFile && it.extension.equals("onnx", ignoreCase = true) }
        val tokensFile = targetDir.walkTopDown().maxDepth(3).firstOrNull { it.isFile && it.name.equals("tokens.txt", ignoreCase = true) }
        val dataDir = targetDir.walkTopDown().maxDepth(3).firstOrNull { it.isDirectory && it.name.equals("espeak-ng-data", ignoreCase = true) }

        if (modelFile == null || tokensFile == null || dataDir == null) {
            _isReady = false
            return@withContext
        }

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = modelFile.absolutePath,
                    lexicon = "",
                    tokens = tokensFile.absolutePath,
                    dataDir = dataDir.absolutePath
                ),
                numThreads = 4,
                debug = false,
                provider = "cpu"
            )
        )

        try {
            tts = OfflineTts(config)
            _isReady = true
        } catch (e: Throwable) {
            e.printStackTrace()
            _isReady = false
        }
    }

    override suspend fun synthesize(text: String, voiceId: String, speed: Float): TtsResult = withContext(Dispatchers.IO) {
        val engine = tts ?: throw IllegalStateException("Sherpa-ONNX 引擎尚未完成初始化")
        
        // 调用 Sherpa-ONNX C++ 底层 JNI 接口生成音频波形
        val audio = engine.generate(text = text, sid = 0, speed = speed)
        TtsResult(
            samples = audio.pcm16Samples,
            sampleRate = audio.sampleRate
        )
    }

    override fun release() {
        tts?.release()
        tts = null
        _isReady = false
    }
}
