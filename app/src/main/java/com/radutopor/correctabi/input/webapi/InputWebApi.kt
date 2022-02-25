package com.radutopor.correctabi.input.webapi

import retrofit2.http.GET

interface InputWebApi {

    @GET(INPUT_DATA)
    suspend fun getInput(): Input

    private companion object {
        const val INPUT_DATA = "uc?export=download&id=1fd7D0NJlhy0NfhsWG00MrCDO04Ekn7my"
    }
}
