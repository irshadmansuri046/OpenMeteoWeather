package com.example.openmeteoweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ambee_air_quality_cache")
data class AmbeeAirQualityEntity(
    @PrimaryKey
    val id: String, // E.g., "lat,lng"
    val latitude: Double,
    val longitude: Double,
    val aqi: Int,
    val pm25: Double,
    val pm10: Double,
    val co: Double,
    val no2: Double,
    val ozone: Double,
    val so2: Double,
    val category: String,
    val dominantPollutant: String,
    val lastUpdated: Long
)
