package com.example.openmeteoweather.data.remote.tomorrow

import com.example.openmeteoweather.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class TomorrowAuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Only apply to tomorrow.io calls
        if (!originalRequest.url.host.contains("tomorrow.io")) {
            return chain.proceed(originalRequest)
        }

        val url = originalRequest.url.newBuilder()
            .addQueryParameter("apikey", BuildConfig.TOMORROW_API_KEY)
            .build()

        val authorisedRequest = originalRequest.newBuilder()
            .url(url)
            .build()

        return chain.proceed(authorisedRequest)
    }
}
