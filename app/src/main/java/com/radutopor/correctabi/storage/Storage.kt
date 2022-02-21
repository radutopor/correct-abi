package com.radutopor.correctabi.storage

import android.content.Context
import androidx.room.Room

object Storage {

    private fun getDatabase(context: Context) =
        Room.databaseBuilder(context, AppDatabase::class.java, "app-database")
            .allowMainThreadQueries()
            .build()

    fun getWordDao(context: Context) = getDatabase(context).wordDao()
}
