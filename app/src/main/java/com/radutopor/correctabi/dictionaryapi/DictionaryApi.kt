package com.radutopor.correctabi.dictionaryapi

interface DictionaryApi {

    suspend fun getEntry(searchTerm: String): DictionaryEntry
}
