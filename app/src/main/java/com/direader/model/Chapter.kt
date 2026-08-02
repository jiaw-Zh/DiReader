package com.direader.model

/**
 * Represents a parsed chapter of a book.
 */
data class Chapter(
    val index: Int,
    val title: String,
    val text: String
)
