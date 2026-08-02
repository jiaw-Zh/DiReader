package com.direader.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.direader.model.Bookmark
import com.direader.model.ReadingProgress
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for reading progress and bookmarks.
 */
@Dao
interface ProgressDao {
    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun getProgress(bookId: String): ReadingProgress?

    @Upsert
    suspend fun saveProgress(progress: ReadingProgress)

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY chapterIndex, sentenceIndex")
    fun getBookmarks(bookId: String): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)
}
