package com.example.openmeteoweather.data.remote.openweather

import com.example.openmeteoweather.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import java.nio.charset.Charset

class OpenWeatherAuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Only apply to openweathermap.org calls
        if (!originalRequest.url.host.contains("openweathermap.org")) {
            return chain.proceed(originalRequest)
        }

        val url = originalRequest.url.newBuilder()
            .addQueryParameter("appid", BuildConfig.OPENWEATHER_API_KEY)
            .build()

        val maskedUrl = url.toString().replace(BuildConfig.OPENWEATHER_API_KEY, "xxxxxx")
        println("OpenWeather_DEBUG: Outgoing Request URL: $maskedUrl")

        val authorisedRequest = originalRequest.newBuilder()
            .url(url)
            .build()

        val response = try {
            chain.proceed(authorisedRequest)
        } catch (e: Exception) {
            println("OpenWeather_DEBUG: OkHttp Request failed: ${e.message}")
            throw e
        }

        val responseCode = response.code
        val responseMessage = response.message
        println("OpenWeather_DEBUG: Response Code: $responseCode, Response Message: $responseMessage")

        val responseBody = response.body
        if (responseBody != null) {
            try {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer
                val responseBodyString = buffer.clone().readString(Charsets.UTF_8)
                println("OpenWeather_DEBUG: Response Body: $responseBodyString")
            } catch (e: Exception) {
                println("OpenWeather_DEBUG: Could not read response body: ${e.message}")
            }
        }

        return response
    }
}
