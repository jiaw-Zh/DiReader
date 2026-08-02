package com.direader.tts

/**
 * Real implementation of [TtsEngine] using sherpa-onnx.
 */
class SherpaOnnxTtsEngine : TtsEngine {
    override val isReady: Boolean
        get() = false

    override val availableVoices: List<VoiceInfo>
        get() = emptyList()

    override suspend fun initialize(modelDir: String) {
        // TODO: Implement with sherpa-onnx API
        throw UnsupportedOperationException("Sherpa-onnx AAR is required to initialize the engine.")
    }

    override suspend fun synthesize(text: String, voiceId: String, speed: Float): TtsResult {
        // TODO: Implement with sherpa-onnx API
        throw UnsupportedOperationException("Sherpa-onnx AAR is required to synthesize speech.")
    }

    override fun release() {
        // TODO: Implement with sherpa-onnx API
        throw UnsupportedOperationException("Sherpa-onnx AAR is required to release the engine.")
    }
}
