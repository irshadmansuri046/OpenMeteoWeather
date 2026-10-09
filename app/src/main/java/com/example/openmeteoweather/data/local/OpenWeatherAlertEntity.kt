package com.example.openmeteoweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "openweather_alert_cache")
data class OpenWeatherAlertEntity(
    @PrimaryKey
    val id: String, // E.g., "lat,lng"
    val latitude: Double,
    val longitude: Double,
    val alertsJson: String, // Stores the list of WeatherAlert serialized to JSON
    val lastUpdated: Long
)
