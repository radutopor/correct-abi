package com.radutopor.correctabi

import android.content.Context
import android.content.SharedPreferences.Editor

class SharedPrefs(context: Context) {

    private val sharedPrefs = context.getSharedPreferences(KEY_SHARED_PREFS, Context.MODE_PRIVATE)

    fun getLevel() = sharedPrefs.getInt(KEY_LEVEL, -1)

    fun setLevel(level: Int) = set { putInt(KEY_LEVEL, level) }

    fun getCoins(startAmount: Int) = sharedPrefs.getInt(KEY_COINS, startAmount)

    fun setCoins(coins: Int) = set { putInt(KEY_COINS, coins) }

    fun getPath() = sharedPrefs.getString(KEY_PATH, null)

    fun setPath(path: String?) = set { putString(KEY_PATH, path) }

    fun wipeEverything() = set {
        remove(KEY_LEVEL)
        remove(KEY_COINS)
        remove(KEY_PATH)
    }

    private fun set(setter: Editor.() -> Editor) = sharedPrefs.edit().setter().apply()

    private companion object {
        const val KEY_SHARED_PREFS = "global-data"
        const val KEY_LEVEL = "level"
        const val KEY_COINS = "coins"
        const val KEY_PATH = "path"
    }
}
