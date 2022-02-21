package com.radutopor.correctabi.input

import com.radutopor.correctabi.GuessablesUtil.getGuessableDictionaryEntry
import com.radutopor.correctabi.GuessablesUtil.toWord
import com.radutopor.correctabi.SharedPrefs
import com.radutopor.correctabi.layerRes
import com.radutopor.correctabi.storage.Storage.getWordDao

class InputController(private val activity: InputActivity) {

    private val wordDao = getWordDao(activity)
    private val sharedPrefs = SharedPrefs(activity)

    suspend fun createRootWord(input: String) {
        val entry = getGuessableDictionaryEntry(input)
        if (entry != null) {
            val newLevel = sharedPrefs.getLevel() + 1
            val path = layerRes[0].indicators[newLevel].toString()
            wordDao.addWord(entry.toWord(path))
            sharedPrefs.setLevel(newLevel)
            activity.startNewGame(path)
        } else {
            activity.showMessage(MSG_WORD_NOT_GUESSABLE)
        }
    }

    fun setCurrentlyOnInput() = sharedPrefs.setPath(null)

    private companion object {
        const val MSG_WORD_NOT_GUESSABLE = "Not good"
    }
}
