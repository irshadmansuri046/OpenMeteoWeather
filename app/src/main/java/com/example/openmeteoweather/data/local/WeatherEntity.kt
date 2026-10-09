package com.example.openmeteoweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_cache")
data class WeatherEntity(
    @PrimaryKey
    val id: String, // E.g., "lat,lng" formatted as a key, or we can use separate fields and a composite primary key. "lat,lng" is simple.
    val latitude: Double,
    val longitude: Double,
    val time: String,
    val temperature: Double,
    val humidity: Double,
    val apparentTemperature: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val precipitation: Double,
    val hourlyTimeJson: String,
    val hourlyTemperaturesJson: String,
    val hourlyWeatherCodesJson: String,
    val hourlyPrecipitationProbabilitiesJson: String,
    val dailyTimeJson: String,
    val dailyWeatherCodesJson: String,
    val dailyTemperaturesMaxJson: String,
    val dailyTemperaturesMinJson: String,
    val dailySunriseJson: String,
    val dailySunsetJson: String,
    val dailyUvIndexMaxJson: String,
    val usAqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val lastUpdated: Long
)
