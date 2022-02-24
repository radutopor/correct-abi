package com.radutopor.correctabi.game

import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.radutopor.correctabi.GuessablesUtil
import com.radutopor.correctabi.GuessablesUtil.Companion.isGuessable
import com.radutopor.correctabi.GuessablesUtil.Companion.toWord
import com.radutopor.correctabi.SharedPrefs
import com.radutopor.correctabi.layerRes
import com.radutopor.correctabi.storage.Storage.getWordDao
import com.radutopor.correctabi.storage.word.Word
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

class GameController(private val activity: GameActivity) {

    private val wordDao = getWordDao(activity)
    private val sharedPrefs = SharedPrefs(activity)
    private val guessables = GuessablesUtil(sharedPrefs)

    suspend fun startGame(path: String, restorePath: String?) {
        val word = wordDao.getWord(path)
        if (word.revealablesNo == -1) {
            createChildWords(word)
        }
        activity.renderGame(word.toGame())

        if (restorePath != null && restorePath != path) {
            val childGamePath = restorePath.take(path.length + 1)
            activity.startNewGame(childGamePath, restorePath)
        }
    }

    private suspend fun createChildWords(word: Word) {
        val revealables = guessables.getRevealableDictionaryEntries(word.definition)
        val maxRevealables = layerRes.maxOf { it.indicators.size }
        val toReveal = min(round(revealables.size * layerDifficulties[word.layer]).toInt(), maxRevealables)
        val freebies = revealables.shuffled().take(revealables.size - toReveal)
        val children = revealables.minus(freebies)
        children.forEachIndexed { index, entry ->
            val childPath = word.path + (layerRes.getOrNull(word.layer + 1)?.indicators?.get(index) ?: index)
            wordDao.addWord(entry.toWord(childPath))
        }
        wordDao.addWord(word.copy(revealablesNo = children.size))
    }

    private fun Word.toGame() = Game(
        lvlIndicators = path.toList(),
        coins = COINS_STRING.format(getCoins()),
        letters = stem.map { it.takeIf { freeLetters.contains(it) } },
        definition = createDefinitionSpannable(this),
    )

    private fun createDefinitionSpannable(word: Word): SpannableString {
        var definition = word.definition
        val childrenAndIndex = wordDao.getChildWords(word.path).map { childWord ->
            val token = childWord.token
            val startIndex = definition.indexOf(token)
            definition = definition.replaceFirst(token, HIDDEN_CHAR.repeat(token.length))
            childWord to startIndex
        }

        val spannable = SpannableString(definition)
        childrenAndIndex.forEach { (childWord, startIndex) ->
            val span = object : ClickableSpan() {
                override fun onClick(textView: View) = processChildWordClick(word, childWord)
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = false
                }
            }
            val endIndex = startIndex + childWord.token.length
            spannable.setSpan(span, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }

    private fun processChildWordClick(word: Word, childWord: Word) {
        if (spendCoins(word.revealCost)) {
            if (deleteWord(childWord)) {
                activity.showMessage(LETTER_GAIN)
            }
            refreshGame(word.path)
        } else if (word.layer < sharedPrefs.getLevel() && childWord.isGuessable()) {
            activity.startNewGame(childWord.path)
        }
    }

    private fun spendCoins(cost: Int): Boolean {
        val minSavings = if (cost > 1) cost else 0
        val currentCoins = getCoins()
        if (currentCoins < cost + minSavings) {
            activity.showMessage(MSG_NOT_ENOUGH_COINS)
            return false
        }
        sharedPrefs.setCoins(currentCoins - cost)
        return true
    }

    fun guessWord(path: String, guess: String) {
        val word = wordDao.getWord(path)
        if (guess == word.stem) {
            processGameWon(word)
        } else {
            activity.showWrongGuess()
        }
    }

    private fun processGameWon(word: Word) {
        sharedPrefs.setGuessedWord(word.token)
        sharedPrefs.setGuessedWord(word.stem)
        val letterAward = deleteWord(word)
        val coinsGained = word.value
        val gains = COINS_GAIN_STRING.format(coinsGained) + if (letterAward) "\n" + LETTER_GAIN else ""
        sharedPrefs.setCoins(getCoins() + coinsGained)
        if (word.layer == 0) {
            val level = sharedPrefs.getLevel()
            if (level == layerRes.size - 1) {
                sharedPrefs.wipeEverything()
                activity.showGameWon()
            } else {
                activity.showLevelComplete(gains, level)
            }
        } else {
            activity.showCorrectGuess(gains)
        }
    }

    private fun deleteWord(word: Word): Boolean {
        val parentPath = word.path.dropLast(1)
        wordDao.getChildWords(parentPath)
            .filter { it.stem == word.stem } // delete current word + any duplicate kin
            .forEach { wordDao.deleteWordTree(it.path) }
        return checkAwardParentLetter(parentPath)
    }

    private fun checkAwardParentLetter(parentPath: String): Boolean {
        if (parentPath.isEmpty()) return false
        val parent = wordDao.getWord(parentPath)
        val revealablesLeftNo = wordDao.getChildWords(parent.path).size
        val shouldAwardLetter = (parent.revealablesNo - revealablesLeftNo) / AWARD_LETTER_FREQ > parent.freeLetters.length
        val unrevealedLetters = parent.stem.toList() - parent.freeLetters.toList()
        return if (shouldAwardLetter && unrevealedLetters.size > 1) {
            wordDao.addWord(parent.copy(freeLetters = parent.freeLetters + unrevealedLetters.random()))
            true
        } else false
    }

    fun refreshGame(path: String) {
        val refreshedGame = wordDao.getWord(path).toGame()
        activity.renderGame(refreshedGame)
    }

    fun setCurrentGame(path: String) = sharedPrefs.setPath(path)

    fun getLayer(path: String) = path.length - 1

    private val Word.layer get() = getLayer(path)

    private val Word.revealCost get() = revealablesPerLayer.subList(layer, sharedPrefs.getLevel()).plus(1).product()

    private val Word.value get() = revealablesPerLayer.subList(layer, sharedPrefs.getLevel() + 1).product()

    private fun List<Int>.product() = reduce { acc, i -> acc * i }

    private fun getCoins() = sharedPrefs.getCoins(startAmount = AVG_NO_REVEALABLES)

    private companion object {
        const val AVG_NO_REVEALABLES = 5 // Average number of revealables per definition
        const val LAYER_DIF_RAMP = 1.00
        val layerDifficulties = layerRes.indices.map { i -> LAYER_DIF_RAMP.pow(i) } // can also specify individually
        val revealablesPerLayer = layerDifficulties.map { round(it * AVG_NO_REVEALABLES).toInt() }

        const val AWARD_LETTER_FREQ = 2

        const val COIN_SYMBOL = "₳"
        const val COINS_STRING = "$COIN_SYMBOL%d"
        const val COINS_GAIN_STRING = "+ $COIN_SYMBOL%d"
        const val LETTER_GAIN = "+ ✉️"

        const val HIDDEN_CHAR = "‒"

        const val MSG_NOT_ENOUGH_COINS = "$COIN_SYMBOL \uD83E\uDD0F"
    }
}
