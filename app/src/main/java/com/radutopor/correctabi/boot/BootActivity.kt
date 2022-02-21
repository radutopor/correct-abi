package com.radutopor.correctabi.boot

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.radutopor.correctabi.game.GameActivity
import com.radutopor.correctabi.input.InputActivity

class BootActivity : AppCompatActivity() {

    private lateinit var controller: BootController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = BootController(this)
        controller.restoreGameState()
    }

    fun startInput() {
        startActivity(Intent(this, InputActivity::class.java))
        finish()
    }

    fun startGame(path: String, restorePath: String) {
        val intent = Intent(this, GameActivity::class.java)
            .putExtra(GameActivity.KEY_PATH, path)
            .putExtra(GameActivity.KEY_RESTORE_PATH, restorePath)
        startActivity(intent)
        finish()
    }
}
