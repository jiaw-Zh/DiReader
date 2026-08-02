package com.direader.tts

import kotlinx.coroutines.delay

/**
 * Stub implementation of [TtsEngine] for development and testing.
 */
class StubTtsEngine : TtsEngine {
    private var _isReady = false
    override val isReady: Boolean
        get() = _isReady

    override val availableVoices: List<VoiceInfo> = listOf(
        VoiceInfo("zf_xiaobei", "小北", "zh"),
        VoiceInfo("zm_yunjian", "云剑", "zh")
    )

    override suspend fun initialize(modelDir: String) {
        delay(500) // Simulate loading time
        _isReady = true
    }

    override suspend fun synthesize(text: String, voiceId: String, speed: Float): TtsResult {
        if (!_isReady) {
            throw IllegalStateException("TtsEngine is not initialized")
        }
        
        // Simulate synthesis time
        delay(100)
        
        // Generate silence proportional to text length (~150ms per char at 24000Hz)
        val sampleRate = 24000
        val durationMs = text.length * 150
        val sampleCount = (sampleRate * (durationMs / 1000.0f)).toInt()
        val samples = ShortArray(sampleCount) { 0 }
        
        return TtsResult(samples, sampleRate)
    }

    override fun release() {
        _isReady = false
    }
}
