package com.radutopor.correctabi.dictionaryapi.freedictionary.dictionary

import retrofit2.http.GET
import retrofit2.http.Path

interface DictionaryWebApi {

    @GET("api/v2/entries/en/{word}")
    suspend fun lookup(@Path("word") word: String): List<Entry>
}