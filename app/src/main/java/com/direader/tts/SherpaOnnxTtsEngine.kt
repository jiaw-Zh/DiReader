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
        val piperDir = File(modelDir, "piper")
        val targetDir = if (piperDir.exists()) piperDir else File(modelDir)

        if (!targetDir.exists()) {
            _isReady = false
            return@withContext
        }

        // 查找 .onnx 模型文件、tokens.txt 及 espeak-ng-data
        val modelFile = targetDir.listFiles()?.firstOrNull { it.extension == "onnx" }
        val tokensFile = File(targetDir, "tokens.txt")
        val dataDir = File(targetDir, "espeak-ng-data")

        if (modelFile == null || !tokensFile.exists() || !dataDir.exists()) {
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
        } catch (e: Exception) {
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
