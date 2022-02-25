package com.radutopor.correctabi.input.webapi

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiBuilder {

    const val BASE_URL = "https://drive.google.com/"

    val inputWebApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(InputWebApi::class.java)
}
