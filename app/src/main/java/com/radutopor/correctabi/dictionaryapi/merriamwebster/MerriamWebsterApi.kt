package com.radutopor.correctabi.dictionaryapi.merriamwebster

import com.radutopor.correctabi.dictionaryapi.DictionaryApi
import com.radutopor.correctabi.dictionaryapi.DictionaryEntry
import com.radutopor.correctabi.dictionaryapi.merriamwebster.dictionary.DictionaryWebApi

class MerriamWebsterApi(private val dictionaryWebApi: DictionaryWebApi) : DictionaryApi {

    override suspend fun getEntry(searchTerm: String): DictionaryEntry {
        val webEntry = dictionaryWebApi.lookup(searchTerm)
            .first { it.partOfSpeech != null && it.definitions.isNotEmpty() }

        var definition = webEntry.definitions.first()
            .trimStart('—')
        definition = abbreviations.filter { definition.contains(it) }
            .fold(definition) { def, abv -> def.replace(abv, abv.replace(".", "")) }
            .split(punctuation).first()
            .trimEnd()

        return DictionaryEntry(
            searchTerm = searchTerm,
            stem = webEntry.meta.stems.first(),
            partOfSpeech = webEntry.partOfSpeech!!,
            definition = definition
        )
    }

    private companion object {
        val abbreviations = listOf("etc.", "e.g.", "i.e.")
        val punctuation = "[.!?;:—+]".toRegex()
    }
}
