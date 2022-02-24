package com.radutopor.correctabi

import android.content.Context
import android.content.SharedPreferences.Editor

class SharedPrefs(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences(KEY_SHARED_PREFS, Context.MODE_PRIVATE)

    fun getLevel() = sharedPrefs.getInt(KEY_LEVEL, -1)

    fun setLevel(level: Int) = set { putInt(KEY_LEVEL, level) }

    fun getCoins(startAmount: Int) = sharedPrefs.getInt(KEY_COINS, startAmount)

    fun setCoins(coins: Int) = set { putInt(KEY_COINS, coins) }

    fun getPath() = sharedPrefs.getString(KEY_PATH, null)

    fun setPath(path: String?) = set { putString(KEY_PATH, path) }

    fun setGuessedWord(word: String) = set { putBoolean(word, true) }

    fun isWordGuessed(word: String) = sharedPrefs.getBoolean(word, false)

    fun wipeEverything() = context.deleteSharedPreferences(KEY_SHARED_PREFS)

    private fun set(setter: Editor.() -> Editor) = sharedPrefs.edit().setter().apply()

    private companion object {
        const val KEY_SHARED_PREFS = "global-data"
        const val KEY_LEVEL = "level"
        const val KEY_COINS = "coins"
        const val KEY_PATH = "path"
    }
}
