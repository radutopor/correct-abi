package com.radutopor.correctabi.game

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import com.radutopor.correctabi.LayerColors
import com.radutopor.correctabi.R
import com.radutopor.correctabi.UIUtils.async
import com.radutopor.correctabi.UIUtils.hideKeyboard
import com.radutopor.correctabi.databinding.GameBinding
import com.radutopor.correctabi.input.InputActivity
import com.radutopor.correctabi.layerRes

class GameActivity : AppCompatActivity() {

    private lateinit var view: GameBinding
    private lateinit var path: String
    private lateinit var colors: LayerColors
    private lateinit var controller: GameController

    private fun getExtra(key: String) = intent.extras!!.getString(key)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = GameBinding.inflate(layoutInflater)
        path = getExtra(KEY_PATH)!!
        controller = GameController(this)

        setContentView(view.root)
        setColors(controller.getLayer(path))
        setStaticListeners()
        async { controller.startGame(path, getExtra(KEY_RESTORE_PATH)) }
    }

    private fun setColors(layer: Int) {
        colors = layerRes[layer].colors
        view.root.setBackgroundResource(colors.base)
        view.navbar.setBackgroundResource(colors.section)
        view.coins.setTextColor(getColor(colors.light))
        view.guessLabel.setTextColor(getColor(colors.section))
        view.thinkLabel.setTextColor(getColor(colors.section))
        view.definition.setTextColor(getColor(colors.dark))
        view.definition.setLinkTextColor(getColor(colors.accent))
        view.letter.backgroundTintList = tint(colors.accent)
        view.letter.setTextColor(getColor(colors.base))
        view.loading.indeterminateTintList = tint(colors.section)
    }

    private fun setStaticListeners() {
        view.definition.movementMethod = LinkMovementMethod.getInstance()
        view.root.setOnClickListener { it.hideKeyboard() }
        view.letter.setOnClickListener { controller.buyLetter(path) }
        view.credits.root.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        controller.setCurrentGame(path)
    }

    fun renderGame(game: Game) = runOnUiThread {
        view.loading.visibility = View.GONE
        view.content.visibility = View.VISIBLE
        setLvlIndicators(game)
        view.coins.text = game.coins
        addLetters(game)
        view.definition.text = game.definition
        view.letter.visibility = if (game.buyLetterVis) View.VISIBLE else View.GONE
    }

    private fun setLvlIndicators(game: Game) {
        view.run {
            listOf(lvlIndicator1, lvlIndicator2, lvlIndicator3, lvlIndicator4, lvlIndicator5)
        }.zip(game.lvlIndicators)
            .forEach { (indicatorView, indicator) -> indicatorView.text = indicator.toString() }
    }

    private fun addLetters(game: Game) {
        view.word.removeAllViews()
        game.letters.forEach { letter ->
            layoutInflater.inflate(R.layout.letter, view.word, true)
            val letterView = view.word.getChildAt(view.word.childCount - 2) as EditText
            letterView.setText(letter?.toString())
            val isEnabled = letterView.text.isBlank()
            letterView.isEnabled = isEnabled
            letterView.backgroundTintList = tint(if (isEnabled) colors.light else colors.section)
            letterView.setTextColor(getColor(colors.accent))
            setLetterViewListeners(letterView)
        }
        view.word.removeViewAt(view.word.childCount - 1)
    }

    private fun setLetterViewListeners(letterView: EditText) {
        letterView.addTextChangedListener {
            val letters = getLetterViews()
            if (letters.all { it.text.isNotBlank() }) {
                view.root.hideKeyboard()
                val word = letters.map { it.text }.joinToString("")
                controller.guessWord(path, word)
            } else {
                letters.first { it.text.isBlank() }.requestFocus()
            }
        }
        letterView.setOnKeyListener { _, _, event ->
            if (event.keyCode != KeyEvent.KEYCODE_DEL || event.action != KeyEvent.ACTION_UP) return@setOnKeyListener false
            val toClear = letterView.takeIf { it.text.isNotBlank() }
                ?: getLetterViews().lastOrNull { it.isEnabled && it.text.isNotBlank() }
            toClear?.apply { text = null }?.requestFocus()
            true
        }
    }

    fun startNewGame(path: String, restorePath: String? = null) {
        val intent = Intent(this, GameActivity::class.java)
            .putExtra(KEY_PATH, path).putExtra(KEY_RESTORE_PATH, restorePath)
        newGameLauncher.launch(intent)
    }

    private val newGameLauncher = registerForActivityResult(StartActivityForResult()) {
        val refreshDelay = if (it.resultCode == Activity.RESULT_OK) DELAY_REFRESH else 0
        view.tapBlocker.visibility = View.VISIBLE
        runDelayed(refreshDelay) {
            view.tapBlocker.visibility = View.GONE
            controller.refreshGame(path)
        }
    }

    fun showMessage(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    fun showWrongGuess(guess: String) {
        val letters = getLetterViews()
        letters.filter { it.isEnabled }.forEach { it.text = null }
        letters.first().requestFocus()
        view.incorrect.guess.text = guess
        view.incorrect.root.visibility = View.VISIBLE
        runDelayed(DELAY_INCORRECT) { view.incorrect.root.visibility = View.GONE }
    }

    fun showCorrectGuess(guess: String, gains: String) {
        populateCorrectGuess(guess, gains)
        runDelayed(DELAY_CORRECT) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun populateCorrectGuess(guess: String, gains: String) {
        view.correct.guess.text = guess
        view.correct.gains.text = gains
        view.correct.root.visibility = View.VISIBLE
    }

    fun showLevelComplete(level: Int, guess: String, gains: String) {
        populateLevelComplete(level, guess, gains)
        view.correct.root.setOnClickListener {
            startActivity(Intent(this, InputActivity::class.java))
            finish()
        }
    }

    fun showGameWon() {
        populateLevelComplete(layerRes.size - 1)
        view.correct.root.setOnClickListener {
            view.credits.root.visibility = View.VISIBLE
        }
    }

    private var mediaPlayer: MediaPlayer? = null

    private fun populateLevelComplete(level: Int, guess: String = "", gains: String = "") {
        populateCorrectGuess(guess, gains)
        view.correct.fanfare.visibility = View.VISIBLE
        mediaPlayer = MediaPlayer.create(this, layerRes[level].fanfare)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun tint(@ColorRes colorRes: Int) = ColorStateList.valueOf(getColor(colorRes))

    private fun getLetterViews() = view.word.children.filterIsInstance<TextView>()

    private fun runDelayed(delay: Long, block: () -> Unit) =
        Handler(Looper.getMainLooper()).postDelayed(block, delay)

    companion object {
        const val KEY_PATH = "path"
        const val KEY_RESTORE_PATH = "restore_path"
        private const val DELAY_CORRECT = 2000L
        private const val DELAY_INCORRECT = 1500L
        private const val DELAY_REFRESH = 800L
    }
}
