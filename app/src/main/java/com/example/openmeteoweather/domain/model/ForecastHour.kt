package com.example.openmeteoweather.domain.model

data class ForecastHour(
    val time: String,
    val temp: Double,
    val weatherCode: Int,
    val rainProbability: Int
)
