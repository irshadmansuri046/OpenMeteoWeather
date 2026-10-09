package com.example.openmeteoweather.domain.model

data class ForecastDay(
    val date: String,
    val tempMin: Double,
    val tempMax: Double,
    val weatherCode: Int,
    val sunrise: String,
    val sunset: String,
    val uvIndex: Double
)
