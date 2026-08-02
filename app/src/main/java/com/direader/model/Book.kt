package com.direader.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a book in the library.
 */
@Entity(tableName = "books")
data class Book(
    @PrimaryKey
    val id: String, // SHA-256 of file
    val title: String,
    val author: String,
    val filePath: String,
    val coverPath: String?,
    val totalChapters: Int,
    val addedAt: Long,
    val lastReadAt: Long
)
