package com.example.openmeteoweather.data.remote.imd

import com.example.openmeteoweather.data.remote.imd.model.IMDAuthRequest
import com.example.openmeteoweather.data.remote.imd.model.IMDTokenResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface IMDAuthService {
    @POST("api/oauth/token.php")
    fun getToken(@Body request: IMDAuthRequest): Call<IMDTokenResponse>
}
