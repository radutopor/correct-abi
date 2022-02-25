package com.radutopor.correctabi.game

import android.text.SpannableString

data class Game(
    val lvlIndicators: List<Char>,
    val coins: String,
    val letters: List<Char?>,
    val definition: SpannableString,
    val buyLetterVis: Boolean,
)
