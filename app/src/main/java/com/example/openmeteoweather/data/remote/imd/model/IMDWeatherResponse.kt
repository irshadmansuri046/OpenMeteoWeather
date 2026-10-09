package com.example.openmeteoweather.data.remote.imd.model

import com.google.gson.annotations.SerializedName

data class IMDWeatherResponse(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("current")
    val current: IMDCurrentWeather?,
    @SerializedName("forecast_hourly")
    val hourly: IMDHourlyForecast?,
    @SerializedName("forecast_daily")
    val daily: IMDDailyForecast?,
    @SerializedName("alerts")
    val alerts: List<IMDAlertDto>?
)

data class IMDCurrentWeather(
    @SerializedName("temp")
    val temp: Double,
    @SerializedName("humidity")
    val humidity: Double,
    @SerializedName("apparent_temp")
    val apparentTemp: Double,
    @SerializedName("wind_speed")
    val windSpeed: Double,
    @SerializedName("precipitation")
    val precipitation: Double,
    @SerializedName("weather_code")
    val weatherCode: Int,
    @SerializedName("time")
    val time: String
)

data class IMDHourlyForecast(
    @SerializedName("times")
    val times: List<String>,
    @SerializedName("temps")
    val temps: List<Double>,
    @SerializedName("weather_codes")
    val weatherCodes: List<Int>,
    @SerializedName("rain_probabilities")
    val rainProbabilities: List<Int>
)

data class IMDDailyForecast(
    @SerializedName("dates")
    val dates: List<String>,
    @SerializedName("weather_codes")
    val weatherCodes: List<Int>,
    @SerializedName("temps_max")
    val tempsMax: List<Double>,
    @SerializedName("temps_min")
    val tempsMin: List<Double>,
    @SerializedName("sunrise")
    val sunrise: List<String>,
    @SerializedName("sunset")
    val sunset: List<String>,
    @SerializedName("uv_index")
    val uvIndex: List<Double>
)

data class IMDAlertDto(
    @SerializedName("title")
    val title: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("severity")
    val severity: String, // "info", "warning", "danger"
    @SerializedName("timing")
    val timing: String
)
