package com.radutopor.correctabi.dictionaryapi.freedictionary

import com.radutopor.correctabi.dictionaryapi.DictionaryApi
import com.radutopor.correctabi.dictionaryapi.DictionaryEntry
import com.radutopor.correctabi.dictionaryapi.freedictionary.dictionary.DictionaryWebApi

class FreeDictionaryWebApi(private val dictionaryWebApi: DictionaryWebApi) : DictionaryApi {

    override suspend fun getEntry(searchTerm: String): DictionaryEntry {
        val webEntry = dictionaryWebApi.lookup(searchTerm).first()
        val meaning = webEntry.meanings.first()
        return DictionaryEntry(
            searchTerm = searchTerm,
            stem = webEntry.word,
            partOfSpeech = meaning.partOfSpeech,
            definition = meaning.definitions.first().definition
                .split(punctuationRegex).first()
        )
    }

    private companion object {
        val punctuationRegex = "[.!?;]".toRegex()
    }
}
