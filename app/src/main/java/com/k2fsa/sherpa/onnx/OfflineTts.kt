package com.k2fsa.sherpa.onnx

import android.content.res.AssetManager

data class OfflineTtsVitsModelConfig(
    var model: String = "",
    var lexicon: String = "",
    var tokens: String = "",
    var dataDir: String = "",
    var noiseScale: Float = 0.667f,
    var noiseScaleW: Float = 0.8f,
    var lengthScale: Float = 1.0f
)

data class OfflineTtsModelConfig(
    var vits: OfflineTtsVitsModelConfig = OfflineTtsVitsModelConfig(),
    var numThreads: Int = 4,
    var debug: Boolean = false,
    var provider: String = "cpu"
)

data class OfflineTtsConfig(
    var model: OfflineTtsModelConfig = OfflineTtsModelConfig(),
    var ruleFsts: String = "",
    var ruleFars: String = "",
    var maxNumSentences: Int = 1
)

class GeneratedAudio(
    val samples: FloatArray,
    val sampleRate: Int
) {
    // 自动将 32-bit float 格式转换为标准 16-bit PCM short 数组
    val pcm16Samples: ShortArray
        get() {
            val shorts = ShortArray(samples.size)
            for (i in samples.indices) {
                val sample = (samples[i] * 32767.0f).coerceIn(-32768.0f, 32767.0f)
                shorts[i] = sample.toInt().toShort()
            }
            return shorts
        }
}

/**
 * Sherpa-ONNX 官方 JNI Kotlin 封装。
 * 直接链接已经打入 APK 中的 libsherpa-onnx-jni.so 动态链接库。
 */
class OfflineTts(
    var config: OfflineTtsConfig
) {
    private var ptr: Long = 0

    init {
        ptr = newOfflineTts(config)
    }

    fun generate(text: String, sid: Int = 0, speed: Float = 1.0f): GeneratedAudio {
        val objArray = generate(ptr, text, sid, speed)
        val samples = objArray[0] as FloatArray
        val sampleRate = objArray[1] as Int
        return GeneratedAudio(samples, sampleRate)
    }

    fun release() {
        if (ptr != 0L) {
            deleteOfflineTts(ptr)
            ptr = 0L
        }
    }

    protected fun finalize() {
        release()
    }

    private external fun newOfflineTts(config: OfflineTtsConfig): Long
    private external fun deleteOfflineTts(ptr: Long)
    private external fun generate(ptr: Long, text: String, sid: Int, speed: Float): Array<Any>

    companion object {
        init {
            try {
                System.loadLibrary("sherpa-onnx-jni")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
