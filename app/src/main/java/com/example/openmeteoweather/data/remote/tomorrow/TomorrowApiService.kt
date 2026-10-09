package com.example.openmeteoweather.data.remote.tomorrow

import retrofit2.http.GET
import retrofit2.http.Query

interface TomorrowApiService {
    @GET("v4/weather/forecast")
    suspend fun getForecast(
        @Query("location") location: String,
        @Query("timesteps") timesteps: String = "1h,1d",
        @Query("units") units: String = "metric"
    ): TomorrowForecastResponse
}
