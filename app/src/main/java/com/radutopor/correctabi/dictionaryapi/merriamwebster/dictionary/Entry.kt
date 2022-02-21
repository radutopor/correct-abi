package com.radutopor.correctabi.dictionaryapi.merriamwebster.dictionary

import com.google.gson.annotations.SerializedName

data class Entry(
    @SerializedName("meta")
    val meta: Meta,
    @SerializedName("fl")
    val partOfSpeech: String?,
    @SerializedName("shortdef")
    val definitions: List<String>
)