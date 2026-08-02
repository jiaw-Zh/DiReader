package com.direader.playback

/**
 * Represents the current status of the audio playback.
 */
enum class PlaybackStatus {
    IDLE, LOADING, PLAYING, PAUSED, ERROR
}

/**
 * Holds the complete state of the playback system.
 */
data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.IDLE,
    val bookId: String = "",
    val bookTitle: String = "",
    val chapterIndex: Int = 0,
    val chapterTitle: String = "",
    val totalChapters: Int = 0,
    val sentenceIndex: Int = 0,
    val totalSentences: Int = 0,
    val currentText: String = "",
    val voiceId: String = "zf_xiaobei",
    val speed: Float = 1.0f,
    val errorMessage: String? = null
)
