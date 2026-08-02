package com.direader.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.direader.model.Book
import com.direader.model.Bookmark
import com.direader.model.ReadingProgress

/**
 * Main application database.
 */
@Database(
    entities = [Book::class, ReadingProgress::class, Bookmark::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun progressDao(): ProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "direader_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
