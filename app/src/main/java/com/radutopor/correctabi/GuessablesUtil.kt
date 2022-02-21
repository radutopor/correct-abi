package com.radutopor.correctabi

import com.radutopor.correctabi.dictionaryapi.DictionaryEntry
import com.radutopor.correctabi.dictionaryapi.merriamwebster.ApiBuilder.dictionaryApi
import com.radutopor.correctabi.storage.word.Word

object GuessablesUtil {

    suspend fun getGuessableDictionaryEntry(token: String): DictionaryEntry? =
        getRevealableDictionaryEntry(token)?.takeIf { isGuessable(it.stem) }

    private suspend fun getRevealableDictionaryEntry(token: String): DictionaryEntry? =
        if (token.matches(revealableRegex) && !lengthyWordBlacklist.contains(token)) {
            try {
                dictionaryApi.getEntry(token).takeIf {
                    it.stem.matches(revealableRegex) && revealablePoS.contains(it.partOfSpeech)
                }
            } catch (e: Exception) {
                null
            }
        } else null

    suspend fun getRevealableDictionaryEntries(definition: String): List<DictionaryEntry> =
        revealableRegex.findAll(definition).toList().map { it.destructured.component1() }
            .mapNotNull { getRevealableDictionaryEntry(it) }

    private fun isGuessable(stem: String) = stem.length <= MAX_LENGTH

    fun Word.isGuessable() = isGuessable(stem)

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
        "they", "them", "theirs", "themselves", "itself", "anybody", "anyone", "anything", "everybody",
        "everyone", "everything", "nobody", "one", "others", "somebody", "someone", "something", "that", "these",
        "this", "those", "what", "whatever", "whichever", "who", "whoever", "whom", "whomever", "whosoever", "you",
        "yours", "yourself", "yourselves", "he", "him", "himself", "his", "she", "her", "hers", "herself",
    )
}
