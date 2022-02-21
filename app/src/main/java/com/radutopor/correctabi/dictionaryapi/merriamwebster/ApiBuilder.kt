package com.radutopor.correctabi.dictionaryapi.merriamwebster

import com.radutopor.correctabi.dictionaryapi.merriamwebster.dictionary.DictionaryWebApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiBuilder {

    private const val BASE_URL = "https://www.dictionaryapi.com/"

    private val dictionaryWebApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(WebApiKeyInterceptor())
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DictionaryWebApi::class.java)

    val dictionaryApi = MerriamWebsterApi(dictionaryWebApi)
}
