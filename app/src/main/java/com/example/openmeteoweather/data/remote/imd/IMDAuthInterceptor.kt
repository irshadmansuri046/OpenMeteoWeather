package com.example.openmeteoweather.data.remote.imd

import com.example.openmeteoweather.BuildConfig
import com.example.openmeteoweather.data.remote.imd.model.IMDAuthRequest
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider

class IMDAuthInterceptor @Inject constructor(
    private val tokenManager: IMDTokenManager,
    private val authServiceProvider: Provider<IMDAuthService>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Only apply authentication to IMD API calls
        if (!originalRequest.url.host.contains("imd.gov.in")) {
            return chain.proceed(originalRequest)
        }

        // Bypass authentication for the token API itself to prevent infinite loop
        if (originalRequest.url.encodedPath.contains("api/oauth/token.php")) {
            return chain.proceed(originalRequest)
        }

        var token = tokenManager.getToken()

        // Auto-fetch token if missing or expired
        if (token == null || tokenManager.isTokenExpired()) {
            synchronized(this) {
                // Double check to prevent multiple concurrent threads from executing token calls
                token = tokenManager.getToken()
                if (token == null || tokenManager.isTokenExpired()) {
                    val newToken = fetchNewToken()
                    if (newToken != null) {
                        token = newToken
                    }
                }
            }
        }

        // Proceed with original request adding X-API-KEY and Authorization headers
        val builder = originalRequest.newBuilder()
            .header("X-API-KEY", BuildConfig.IMD_API_KEY)
        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }
        val authorisedRequest = builder.build()

        println("IMD_DEBUG: Outgoing Request URL: ${authorisedRequest.url}")
        println("IMD_DEBUG: Outgoing Headers: X-API-KEY=${BuildConfig.IMD_API_KEY.take(5)}..., AuthPresent=${token != null}")
        
        val response = chain.proceed(authorisedRequest)
        println("IMD_DEBUG: Response Code: ${response.code}, Response Message: ${response.message}")

        // Retry once if token is expired on the server (e.g. returns 401 Unauthorized)
        if (response.code == 401) {
            synchronized(this) {
                // Clear expired token and fetch fresh one
                tokenManager.clearToken()
                val newToken = fetchNewToken()
                if (newToken != null) {
                    response.close() // Close the previous failed response
                    val newRequest = originalRequest.newBuilder()
                        .header("X-API-KEY", BuildConfig.IMD_API_KEY)
                        .header("Authorization", "Bearer $newToken")
                        .build()
                    return chain.proceed(newRequest)
                }
            }
        }

        return response
    }

    private fun fetchNewToken(): String? {
        return try {
            val authService = authServiceProvider.get()
            val requestBody = IMDAuthRequest(
                email = BuildConfig.IMD_EMAIL,
                password = BuildConfig.IMD_PASSWORD
            )
            val call = authService.getToken(requestBody)
            val response = call.execute()
            if (response.isSuccessful) {
                val tokenResponse = response.body()
                if (tokenResponse != null) {
                    tokenManager.saveToken(
                        token = tokenResponse.accessToken,
                        expiresIn = tokenResponse.expiresIn ?: 3600L
                    )
                    tokenResponse.accessToken
                } else null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
