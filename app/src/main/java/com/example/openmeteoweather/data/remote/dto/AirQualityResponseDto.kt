package com.example.openmeteoweather.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AirQualityResponseDto(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("current") val current: CurrentAirQualityDto?
)

data class CurrentAirQualityDto(
    @SerializedName("time") val time: String,
    @SerializedName("us_aqi") val usAqi: Int?,
    @SerializedName("pm2_5") val pm25: Double?,
    @SerializedName("pm10") val pm10: Double?
)
