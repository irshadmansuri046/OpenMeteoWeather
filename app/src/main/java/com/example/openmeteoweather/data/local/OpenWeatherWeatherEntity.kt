package com.example.openmeteoweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "openweather_weather_cache")
data class OpenWeatherWeatherEntity(
    @PrimaryKey
    val id: String, // E.g., "lat,lng"
    val latitude: Double,
    val longitude: Double,
    val weatherInfoJson: String, // Stores the complete WeatherInfo serialized to JSON
    val lastUpdated: Long
)
