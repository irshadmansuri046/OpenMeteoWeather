package com.example.openmeteoweather.data.remote.ambee

import com.example.openmeteoweather.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AmbeeAuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Only apply authentication to Ambee API calls
        if (!originalRequest.url.host.contains("ambeedata.com")) {
            return chain.proceed(originalRequest)
        }

        val authorisedRequest = originalRequest.newBuilder()
            .header("x-api-key", BuildConfig.AMBEE_API_KEY)
            .header("Content-Type", "application/json")
            .build()

        return chain.proceed(authorisedRequest)
    }
}
