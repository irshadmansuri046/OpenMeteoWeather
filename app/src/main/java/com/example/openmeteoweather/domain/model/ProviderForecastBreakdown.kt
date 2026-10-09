package com.example.openmeteoweather.domain.model

data class ProviderForecastBreakdown(
    val providerName: String,
    val rainProbability: Int,
    val temp: Double,
    val windSpeed: Double,
    val humidity: Double,
    val isSuccess: Boolean
)
