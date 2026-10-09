package com.example.openmeteoweather.data.remote.swan

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Body

interface SwanWeatherApiService {
    @GET("weather/full")
    suspend fun getFullWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): SwanWeatherResponse

    @GET("weather/crops")
    suspend fun getCrops(): List<SwanCropDto>

    @GET("weather/crops/{crop_id}/stages")
    suspend fun getCropStages(
        @Path("crop_id") cropId: Int
    ): List<SwanCropStageDto>

    @POST("weather/custom")
    suspend fun getCustomWeather(
        @Body request: SwanCustomWeatherRequest
    ): SwanCustomWeatherResponse
}
