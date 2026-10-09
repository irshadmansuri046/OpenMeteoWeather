package com.example.openmeteoweather.data.remote.ambee

import com.google.gson.annotations.SerializedName

// 1. Weather Latest DTOs
data class AmbeeWeatherLatestResponse(
    val message: String,
    val data: AmbeeWeatherLatestData?
)

data class AmbeeWeatherLatestData(
    val time: Long,
    val cloudCover: Double?,
    val dewPoint: Double?,
    val humidity: Double?,
    val pressure: Double?,
    val precipIntensity: Double?,
    val temperature: Double?,
    val apparentTemperature: Double?,
    val windSpeed: Double?,
    val uvIndex: Double?,
    val icon: String?,
    val summary: String?
)

// 2. Weather Forecast DTOs
data class AmbeeWeatherForecastResponse(
    val message: String,
    val data: AmbeeWeatherForecastData?
)

data class AmbeeWeatherForecastData(
    val forecast: List<AmbeeWeatherForecastItem>?
)

data class AmbeeWeatherForecastItem(
    val time: Long,
    val temperature: Double?,
    val apparentTemperature: Double?,
    val cloudCover: Double?,
    val dewPoint: Double?,
    val humidity: Double?,
    val pressure: Double?,
    val precipIntensity: Double?,
    val precipProbability: Double?, // Sometimes double, we'll parse to Double and cast safely
    val windSpeed: Double?,
    val uvIndex: Double?,
    val icon: String?,
    val summary: String?
)

// 3. Air Quality DTOs
data class AmbeeAirQualityResponse(
    val message: String,
    val stations: List<AmbeeAirQualityStation>?
)

data class AmbeeAirQualityStation(
    val AQI: Int?,
    @SerializedName("PM25") val pm25: Double?,
    @SerializedName("PM10") val pm10: Double?,
    @SerializedName("CO") val co: Double?,
    @SerializedName("NO2") val no2: Double?,
    @SerializedName("OZONE") val ozone: Double?,
    @SerializedName("SO2") val so2: Double?,
    val aqiInfo: AmbeeAqiInfo?,
    val city: String?,
    val placeName: String?
)

data class AmbeeAqiInfo(
    val pollutant: String?,
    val category: String?
)

// 4. Soil Moisture DTOs
data class AmbeeSoilResponse(
    val message: String,
    val data: List<AmbeeSoilDataItem>?
)

data class AmbeeSoilDataItem(
    val soilMoisture: Double?,
    val soilTemperature: Double?,
    val lat: Double?,
    val lng: Double?
)
