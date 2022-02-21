package com.radutopor.correctabi.dictionaryapi.freedictionary

import com.radutopor.correctabi.dictionaryapi.freedictionary.dictionary.DictionaryWebApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiBuilder {

    const val BASE_URL = "https://api.dictionaryapi.dev/"

    private val dictionaryWebApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DictionaryWebApi::class.java)

    val dictionaryApi = FreeDictionaryWebApi(dictionaryWebApi)
}
