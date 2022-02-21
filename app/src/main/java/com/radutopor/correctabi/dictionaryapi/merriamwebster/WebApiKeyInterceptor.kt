package com.radutopor.correctabi.dictionaryapi.merriamwebster

import okhttp3.Interceptor
import okhttp3.Response

class WebApiKeyInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val url = request.url().newBuilder().addQueryParameter("key", API_KEY).build()
        request = request.newBuilder().url(url).build()
        return chain.proceed(request)
    }

    companion object {
        const val API_KEY = "495b8ee1-71bb-4322-933d-aaa9bca72f53"
    }
}
