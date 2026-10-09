package com.example.openmeteoweather.domain.model

data class WeatherAlert(
    val title: String,
    val description: String,
    val severity: String, // "INFO", "WARNING", "DANGER"
    val timing: String
)
