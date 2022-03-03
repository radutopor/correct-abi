package com.radutopor.correctabi

import com.radutopor.correctabi.dictionaryapi.DictionaryEntry
import com.radutopor.correctabi.dictionaryapi.merriamwebster.ApiBuilder.dictionaryApi
import com.radutopor.correctabi.storage.word.Word
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class GuessablesUtil(private val sharedPrefs: SharedPrefs) {

    suspend fun getGuessableDictionaryEntry(token: String): DictionaryEntry? =
        getRevealableDictionaryEntry(token)?.takeIf { isGuessable(it.stem) }

    private suspend fun getRevealableDictionaryEntry(token: String): DictionaryEntry? =
        if (!lengthyWordBlacklist.contains(token) && !sharedPrefs.isWordGuessed(token) && token.matches(revealableRegex)) {
            try {
                dictionaryApi.getEntry(token).takeIf {
                    val isStemGuessed = sharedPrefs.isWordGuessed(it.stem)
                    if (isStemGuessed) {
                        sharedPrefs.setGuessedWord(token)
                    }
                    !isStemGuessed && revealablePoS.contains(it.partOfSpeech) && it.stem.matches(revealableRegex)
                }
            } catch (e: Exception) {
                null
            }
        } else null

    suspend fun getRevealableDictionaryEntries(definition: String): List<DictionaryEntry> = coroutineScope {
        revealableRegex.findAll(definition).toList().map { it.destructured.component1() }
            .map { async { getRevealableDictionaryEntry(it) } }.awaitAll()
            .filterNotNull()
    }

    companion object {

        fun Word.isGuessable() = isGuessable(stem)

        private fun isGuessable(stem: String) = stem.length <= MAX_LENGTH

        fun DictionaryEntry.toWord(path: String) =
            Word(
                path = path,
                token = searchTerm,
                stem = stem,
                definition = definition
            )

        private const val MIN_LENGTH = 3
        private const val MAX_LENGTH = 10
        private val revealableRegex = "\\b([A-Za-z][a-z]{${MIN_LENGTH - 1},})\\b".toRegex()
        private val revealablePoS = listOf("noun", "verb", "adjective", "adverb", "preposition")
        private val lengthyWordBlacklist = listOf(
            "the", "and", "not", "but", "for", "with", "without", "etc", "unless",
            // pronouns
            "they", "them", "theirs", "themselves", "itself", "anybody", "anyone", "anything", "everybody", "everyone", "everything",
            "nobody", "one", "others", "somebody", "someone", "something", "that", "these", "this", "those", "what", "whatever",
            "whichever", "who", "whoever", "whom", "whomever", "whosoever", "you", "yours", "yourself", "yourselves", "he", "him",
            "himself", "his", "she", "her", "hers", "herself", "your", "its"
        )
    }
}
