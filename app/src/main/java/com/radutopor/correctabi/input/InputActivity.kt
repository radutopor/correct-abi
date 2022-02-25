package com.radutopor.correctabi.input

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.radutopor.correctabi.UIUtils.async
import com.radutopor.correctabi.UIUtils.hideKeyboard
import com.radutopor.correctabi.databinding.InputBinding
import com.radutopor.correctabi.game.GameActivity

class InputActivity : AppCompatActivity() {

    private lateinit var view: InputBinding
    private lateinit var controller: InputController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = InputBinding.inflate(layoutInflater)
        controller = InputController(this)

        setContentView(view.root)
        setListeners()

        showLoading()
        async { controller.onCreate() }
    }

    private fun setListeners() {
        view.showInput.setOnClickListener { showInput() }
        view.submit.setOnClickListener {
            it.hideKeyboard()
            showLoading()
            async { controller.submitInput(view.inputWord.text.toString()) }
        }
    }

    private fun showLoading() {
        view.prompt.visibility = View.GONE
        view.input.visibility = View.GONE
        view.loading.visibility = View.VISIBLE
    }

    fun showPrompt() = runOnUiThread {
        view.loading.visibility = View.GONE
        view.prompt.visibility = View.VISIBLE
    }

    fun showInput(message: String? = null) = runOnUiThread {
        view.loading.visibility = View.GONE
        view.prompt.visibility = View.GONE
        view.input.visibility = View.VISIBLE
        view.inputWord.text.clear()
        message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
    }

    fun startNewGame(path: String) {
        startActivity(Intent(this, GameActivity::class.java).putExtra(GameActivity.KEY_PATH, path))
        finish()
    }
}
