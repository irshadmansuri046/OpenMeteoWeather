package com.example.openmeteoweather.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WeatherResponseDto(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("current") val current: CurrentWeatherDto?,
    @SerializedName("hourly") val hourly: HourlyForecastDto?,
    @SerializedName("daily") val daily: DailyForecastDto?
)

data class CurrentWeatherDto(
    @SerializedName("time") val time: String,
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("relative_humidity_2m") val humidity: Double?,
    @SerializedName("apparent_temperature") val apparentTemperature: Double?,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("wind_speed_10m") val windSpeed: Double?,
    @SerializedName("precipitation") val precipitation: Double?
)

data class HourlyForecastDto(
    @SerializedName("time") val time: List<String>,
    @SerializedName("temperature_2m") val temperatures: List<Double>,
    @SerializedName("weather_code") val weatherCodes: List<Int>,
    @SerializedName("precipitation_probability") val precipitationProbabilities: List<Int>?
)

data class DailyForecastDto(
    @SerializedName("time") val time: List<String>,
    @SerializedName("weather_code") val weatherCodes: List<Int>,
    @SerializedName("temperature_2m_max") val temperaturesMax: List<Double>,
    @SerializedName("temperature_2m_min") val temperaturesMin: List<Double>,
    @SerializedName("sunrise") val sunrise: List<String>?,
    @SerializedName("sunset") val sunset: List<String>?,
    @SerializedName("uv_index_max") val uvIndexMax: List<Double>?
)
