package com.example.openmeteoweather.data.remote.ambee

import retrofit2.http.GET
import retrofit2.http.Query

interface AmbeeApiService {

    @GET("weather/latest/by-lat-lng")
    suspend fun getLatestWeather(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("units") units: String = "si"
    ): AmbeeWeatherLatestResponse

    @GET("weather/forecast/by-lat-lng")
    suspend fun getWeatherForecast(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("units") units: String = "si"
    ): AmbeeWeatherForecastResponse

    @GET("latest/by-lat-lng")
    suspend fun getAirQuality(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): AmbeeAirQualityResponse

    @GET("soil/latest/by-lat-lng")
    suspend fun getSoilMoisture(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): AmbeeSoilResponse
}
