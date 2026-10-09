package com.example.openmeteoweather.data.remote.imd

import com.example.openmeteoweather.data.remote.imd.model.IMDWeatherResponse
import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface IMDApiService {
    @GET("api/weather/forecast.php")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): IMDWeatherResponse

    @GET("api/v1/aws_data")
    suspend fun getAwsData(
        @Query("sid") stateId: Int? = null
    ): JsonElement
}
