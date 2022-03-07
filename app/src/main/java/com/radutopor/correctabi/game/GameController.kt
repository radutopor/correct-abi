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
import kotlin.math.max
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

        if (restorePath != null) {
            if (restorePath != path) {
                val nextChildPath = restorePath.take(path.length + 1)
                activity.startNewGame(nextChildPath, restorePath)
            }
        } else if (!canAfford(word.revealCost) && !word.isLeaf) {
            wordDao.getChildWords(word.path).randomOrNull()?.let {
                activity.startNewGame(it.path)
            }
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
        buyLetterVis = wordDao.getChildWords(path).isEmpty() && unrevealedLetters.size > 1
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
        } else if (!word.isLeaf && childWord.isGuessable()) {
            activity.startNewGame(childWord.path)
        }
    }

    fun buyLetter(path: String) {
        val word = wordDao.getWord(path)
        if (spendCoins(word.revealCost)) {
            addFreeLetter(word)
            refreshGame(word.path)
        }
    }

    private fun spendCoins(cost: Int): Boolean {
        if (canAfford(cost)) {
            sharedPrefs.setCoins(getCoins() - cost)
            return true
        }
        activity.showMessage(NOT_ENOUGH_COINS)
        return false
    }

    private fun canAfford(cost: Int): Boolean {
        val minSavings = if (cost > 1) AVG_NO_REVEALABLES else 0
        return getCoins() >= cost + minSavings
    }

    fun guessWord(path: String, guess: String) {
        val word = wordDao.getWord(path)
        if (guess == word.stem) {
            processGameWon(word)
        } else {
            activity.showWrongGuess(GUESS_STRING.format(guess))
        }
    }

    private fun processGameWon(word: Word) {
        sharedPrefs.setGuessedWord(word.token)
        sharedPrefs.setGuessedWord(word.stem)
        val letterAward = deleteWord(word)
        val coinsGained = word.value
        val gains = COINS_GAIN_STRING.format(coinsGained) + if (letterAward) "\n" + LETTER_GAIN else ""
        sharedPrefs.setCoins(getCoins() + coinsGained)
        val guess = GUESS_STRING.format(word.stem)
        if (word.layer == 0) {
            val level = sharedPrefs.getLevel()
            if (level == layerRes.size - 1) {
                sharedPrefs.wipeEverything()
                activity.showGameWon()
            } else {
                activity.showLevelComplete(level, guess, gains)
            }
        } else {
            activity.showCorrectGuess(guess, gains)
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
        val awardLetterFreq = max(2, round(parent.revealablesNo / (parent.stem.toList().distinct().size - 1f)).toInt())
        val shouldAwardLetter = (parent.revealablesNo - revealablesLeftNo) / awardLetterFreq > parent.freeLetters.length
        return if (shouldAwardLetter && parent.unrevealedLetters.size > 1) {
            addFreeLetter(parent)
            true
        } else false
    }

    private fun addFreeLetter(word: Word) {
        val newFreeLetters = word.freeLetters + word.unrevealedLetters.random()
        wordDao.addWord(word.copy(freeLetters = newFreeLetters))
    }

    fun refreshGame(path: String) {
        val refreshedGame = wordDao.getWord(path).toGame()
        activity.renderGame(refreshedGame)
    }

    fun setCurrentGame(path: String) = sharedPrefs.setPath(path)

    fun getLayer(path: String) = path.length - 1

    private val Word.layer get() = getLayer(path)

    private val Word.isLeaf get() = layer == sharedPrefs.getLevel()

    private val Word.unrevealedLetters get() = (stem.toList() - freeLetters.toList()).distinct()

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
        const val COINS_GAIN_STRING = "+ $COIN_SYMBOL%d"
        const val LETTER_GAIN = "+ ✉️"
        const val HIDDEN_CHAR = "‒"
        const val NOT_ENOUGH_COINS = "$COIN_SYMBOL \uD83E\uDD0F"
        const val GUESS_STRING = "%s is"
    }
}
