package com.direader.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a user bookmark in a book.
 */
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: String,
    val chapterIndex: Int,
    val sentenceIndex: Int,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
