package com.direader.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores reading and TTS progress for a specific book.
 */
@Entity(tableName = "reading_progress")
data class ReadingProgress(
    @PrimaryKey
    val bookId: String,
    val chapterIndex: Int = 0,
    val sentenceIndex: Int = 0,
    val voiceId: String = "zf_xiaobei",
    val speed: Float = 1.0f,
    val updatedAt: Long
)
