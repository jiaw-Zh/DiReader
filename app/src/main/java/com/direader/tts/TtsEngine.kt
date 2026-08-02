package com.direader.tts

/**
 * Information about a TTS voice.
 */
data class VoiceInfo(
    val id: String,
    val name: String,
    val language: String
)

/**
 * Result of a TTS synthesis operation.
 */
class TtsResult(
    val samples: ShortArray,
    val sampleRate: Int
)

/**
 * Interface for Text-To-Speech engine.
 */
interface TtsEngine {
    /**
     * Whether the engine is ready to synthesize speech.
     */
    val isReady: Boolean

    /**
     * List of available voices.
     */
    val availableVoices: List<VoiceInfo>

    /**
     * Initializes the TTS engine.
     *
     * @param modelDir The directory containing the models.
     */
    suspend fun initialize(modelDir: String)

    /**
     * Synthesizes text to audio.
     *
     * @param text The text to synthesize.
     * @param voiceId The ID of the voice to use.
     * @param speed The speaking speed.
     * @return The synthesis result.
     */
    suspend fun synthesize(text: String, voiceId: String, speed: Float = 1.0f): TtsResult

    /**
     * Releases resources used by the engine.
     */
    fun release()
}
