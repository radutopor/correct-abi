package com.radutopor.correctabi

import androidx.annotation.ColorRes
import androidx.annotation.RawRes

data class LayerRes(
    val indicators: List<Char>,
    val colors: LayerColors,
    @RawRes val fanfare: Int
)

data class LayerColors(
    @ColorRes val base: Int,
    @ColorRes val section: Int,
    @ColorRes val light: Int,
    @ColorRes val dark: Int,
    @ColorRes val accent: Int
)

val layerRes = listOf(
    LayerRes(
        listOf('1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '+', '-', '%', '*', '/', '<', '>'),
        LayerColors(R.color.base_1, R.color.section_1, R.color.light_1, R.color.dark_1, R.color.accent_1),
        R.raw.fanfare_1
    ),
    LayerRes(
        listOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q'),
        LayerColors(R.color.base_2, R.color.section_2, R.color.light_2, R.color.dark_2, R.color.accent_2),
        R.raw.fanfare_2
    ),
    LayerRes(
        listOf('α', 'β', 'γ', 'δ', 'ε', 'ζ', 'η', 'θ', 'ι', 'κ', 'λ', 'μ', 'ν', 'ξ', 'ο', 'π', 'ρ'),
        LayerColors(R.color.base_3, R.color.section_3, R.color.light_3, R.color.dark_3, R.color.accent_3),
        R.raw.fanfare_3
    ),
    LayerRes(
        listOf('つ', 'く', 'ひ', 'の', 'わ', 'へ', 'ち', 'ん', 'り', 'ろ', 'し', 'ぬ', 'ふ', 'う', 'ぐ', 'を', 'ま'),
        LayerColors(R.color.base_4, R.color.section_4, R.color.light_4, R.color.dark_4, R.color.accent_4),
        R.raw.fanfare_4
    ),
    LayerRes(
        listOf('☂', '☭', '✂', '⚢', '☃', '☄', '☏', '♻', '⚖', '✐', '☢', '☮', '✈', '♫', '♞', '☘', '♠'),
        LayerColors(R.color.base_5, R.color.section_5, R.color.light_5, R.color.dark_5, R.color.accent_5),
        R.raw.fanfare_5
    ),
)
