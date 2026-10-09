package com.example.openmeteoweather.domain.provider

import com.example.openmeteoweather.domain.model.WeatherInfo

interface WeatherProvider {
    suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherInfo
}
