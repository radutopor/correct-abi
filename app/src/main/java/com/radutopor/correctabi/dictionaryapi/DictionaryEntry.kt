package com.radutopor.correctabi.dictionaryapi

data class DictionaryEntry(
    val searchTerm: String,
    val stem: String,
    val partOfSpeech: String,
    val definition: String
)
