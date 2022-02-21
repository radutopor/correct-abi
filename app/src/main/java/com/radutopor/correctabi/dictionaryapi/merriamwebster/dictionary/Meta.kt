package com.radutopor.correctabi.dictionaryapi.merriamwebster.dictionary

import com.google.gson.annotations.SerializedName

data class Meta(
    @SerializedName("stems")
    val stems: List<String>
)