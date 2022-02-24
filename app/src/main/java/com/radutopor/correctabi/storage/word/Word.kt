package com.radutopor.correctabi.storage.word

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Word(
    @PrimaryKey val path: String,
    val token: String,
    val stem: String,
    val freeLetters: String = "",
    val definition: String,
    val revealablesNo: Int = -1
)
