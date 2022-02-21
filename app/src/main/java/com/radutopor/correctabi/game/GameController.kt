package com.radutopor.correctabi.game

import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import com.radutopor.correctabi.GuessablesUtil.getRevealableDictionaryEntries
import com.radutopor.correctabi.GuessablesUtil.isGuessable
import com.radutopor.correctabi.GuessablesUtil.toWord
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

    suspend fun startGame(path: String, restorePath: String?) {
        val word = wordDao.getWord(path)
        if (!word.areChildrenCreated) {
            createChildWords(word)
        }
        activity.renderGame(word.toGame())

        if (restorePath != null && restorePath != path) {
            val childGamePath = restorePath.take(path.length + 1)
            activity.startNewGame(childGamePath, restorePath)
        }
    }

    private suspend fun createChildWords(word: Word) {
        val revealables = getRevealableDictionaryEntries(word.definition)
        val maxRevealables = layerRes.maxOf { it.indicators.size }
        val toReveal = min(round(revealables.size * layerDifficulties[word.layer]).toInt(), maxRevealables)
        val freebies = revealables.shuffled().take(revealables.size - toReveal)
        revealables.minus(freebies)
            .forEachIndexed { index, entry ->
                val childPath = word.path + (layerRes.getOrNull(word.layer + 1)?.indicators?.get(index) ?: index)
                wordDao.addWord(entry.toWord(childPath))
            }
        wordDao.addWord(word.copy(areChildrenCreated = true))
    }

    private fun Word.toGame() = Game(
        lvlIndicators = path.toList(),
        coins = COINS_STRING.format(getCoins()),
        letters = stem.map { it.takeIf { lettersGuessed.contains(it) } },
        definition = createDefinitionSpannable(this),
        lettersGuessed = lettersGuessed.toList().joinToString(", "),
        letterCost = COST_STRING.format(revealCost)
    )

    private fun createDefinitionSpannable(word: Word): SpannableString {
        var definition = word.definition
        val childrenAndIndex = wordDao.getChildWords(word.path).map { childWord ->
            val token = childWord.token
            val startIndex = definition.indexOf(token)
            definition = definition.replaceFirst(token, hiddenChar.repeat(token.length))
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
        if (word.layer < sharedPrefs.getLevel() && childWord.isGuessable()) {
            activity.startNewGame(childWord.path)
        } else if (spendCoins(word.revealCost)) {
            deleteWord(childWord)
            activity.renderGame(word.toGame())
        }
    }

    fun guessLetter(path: String, letter: String) {
        if (letter.isBlank()) return
        var word = wordDao.getWord(path)
        if (word.lettersGuessed.contains(letter)) {
            activity.showMessage(MSG_LETTER_ALREADY_GUESSED)
        } else if (spendCoins(word.revealCost)) {
            word = word.copy(lettersGuessed = word.lettersGuessed + letter)
            wordDao.addWord(word)
            if (word.lettersGuessed.toList().containsAll(word.stem.toList())) {
                processGameWon(word)
            } else {
                if (!word.stem.contains(letter)) {
                    activity.showMessage(MSG_WRONG_LETTER_GUESS)
                }
                activity.renderGame(word.toGame())
            }
        }
    }

    private fun spendCoins(cost: Int): Boolean {
        val currentCoins = getCoins()
        if (cost > currentCoins) {
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
        deleteWord(word)
        val coinsGained = word.value
        sharedPrefs.setCoins(getCoins() + coinsGained)
        if (word.layer == 0) {
            val level = sharedPrefs.getLevel()
            if (level == layerRes.size - 1) {
                sharedPrefs.wipeEverything()
                activity.showGameWon()
            } else {
                activity.showLevelComplete(GAINS_STRING.format(coinsGained), level)
            }
        } else {
            activity.showCorrectGuess(GAINS_STRING.format(coinsGained))
        }
    }

    private fun deleteWord(word: Word) {
        wordDao.getChildWords(word.path.dropLast(1))
            .filter { it.stem == word.stem } // delete current word + any duplicate kin
            .forEach { wordDao.deleteWordTree(it.path) }
    }

    fun refreshGame(path: String) {
        val refreshedGame = wordDao.getWord(path).toGame()
        activity.renderGame(refreshedGame)
    }

    fun setCurrentGame(path: String) = sharedPrefs.setPath(path)

    private val Word.layer get() = getLayer(path)

    fun getLayer(path: String) = path.length - 1

    private val Word.revealCost get() = revealablesPerLayer.subList(layer, sharedPrefs.getLevel()).plus(1).product()

    private val Word.value get() = revealablesPerLayer.subList(layer, sharedPrefs.getLevel() + 1).product()

    private fun List<Int>.product() = reduce { acc, i -> acc * i }

    private fun getCoins() = sharedPrefs.getCoins(startAmount = AVG_NO_REVEALABLES)

    private companion object {
        const val AVG_NO_REVEALABLES = 5 // Average number of revealables per definition
        const val LAYER_DIF_RAMP = 1.00
        val layerDifficulties = layerRes.indices.map { i -> LAYER_DIF_RAMP.pow(i) } // can also specify individually
        val revealablesPerLayer = layerDifficulties.map { round(it * AVG_NO_REVEALABLES).toInt() }

        const val COIN_SYMBOL = "₳"
        const val COINS_STRING = "$COIN_SYMBOL%d"
        const val COST_STRING = "($COIN_SYMBOL-%d)"
        const val GAINS_STRING = "+ $COIN_SYMBOL%d"

        const val hiddenChar = "‒"

        const val MSG_WRONG_LETTER_GUESS = "No"
        const val MSG_LETTER_ALREADY_GUESSED = "Still no"
        const val MSG_NOT_ENOUGH_COINS = "$COIN_SYMBOL \uD83E\uDD0F"
    }
}
