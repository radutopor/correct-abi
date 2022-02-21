package com.radutopor.correctabi.boot

import com.radutopor.correctabi.SharedPrefs

class BootController(private val activity: BootActivity) {

    private val sharedPrefs = SharedPrefs(activity)

    fun restoreGameState() {
        val restorePath = sharedPrefs.getPath()
        if (restorePath == null) {
            activity.startInput()
        } else {
            activity.startGame(restorePath.take(1), restorePath)
        }
    }
}
