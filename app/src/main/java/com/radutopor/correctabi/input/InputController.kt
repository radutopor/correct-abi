package com.radutopor.correctabi.input

import com.radutopor.correctabi.GuessablesUtil
import com.radutopor.correctabi.GuessablesUtil.Companion.toWord
import com.radutopor.correctabi.SharedPrefs
import com.radutopor.correctabi.input.webapi.ApiBuilder.inputWebApi
import com.radutopor.correctabi.layerRes
import com.radutopor.correctabi.storage.Storage.getWordDao

class InputController(private val activity: InputActivity) {

    private val wordDao = getWordDao(activity)
    private val sharedPrefs = SharedPrefs(activity)
    private val guessables = GuessablesUtil(sharedPrefs)

    suspend fun onCreate() {
        sharedPrefs.setPath(null)
        val webWord = inputWebApi.getInput().word
        if (!tryStartWithInput(webWord)) {
            activity.showPrompt()
        }
    }

    suspend fun submitInput(input: String) {
        if (!tryStartWithInput(input)) {
            activity.showInput(MSG_WORD_NOT_GUESSABLE)
        }
    }

    private suspend fun tryStartWithInput(input: String): Boolean {
        val entry = guessables.getGuessableDictionaryEntry(input) ?: return false
        val newLevel = sharedPrefs.getLevel() + 1
        val path = layerRes[0].indicators[newLevel].toString()
        wordDao.addWord(entry.toWord(path))
        sharedPrefs.setLevel(newLevel)
        activity.startNewGame(path)
        return true
    }

    private companion object {
        const val MSG_WORD_NOT_GUESSABLE = "Not good"
    }
}
