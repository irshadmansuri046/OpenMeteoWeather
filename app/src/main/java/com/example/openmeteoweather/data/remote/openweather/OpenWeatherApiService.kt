package com.example.openmeteoweather.data.remote.openweather

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenWeatherApiService {
    @GET("data/3.0/onecall")
    suspend fun getOneCallForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric",
        @Query("exclude") exclude: String = "minutely"
    ): OpenWeatherOneCallResponse

    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric"
    ): OpenWeatherCurrentResponse

    @GET("data/2.5/forecast")
    suspend fun getForecast25(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String = "metric"
    ): OpenWeatherForecastResponse
}
