package com.radutopor.correctabi.dictionaryapi.merriamwebster.dictionary

import retrofit2.http.GET
import retrofit2.http.Path

interface DictionaryWebApi {

    @GET("api/v3/references/learners/json/{word}")
    suspend fun lookup(@Path("word") word: String): List<Entry>
}
