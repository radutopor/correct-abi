package com.radutopor.correctabi.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import com.radutopor.correctabi.storage.word.Word
import com.radutopor.correctabi.storage.word.WordDao

@Database(entities = [Word::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wordDao(): WordDao
}
