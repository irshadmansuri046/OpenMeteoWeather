package com.example.openmeteoweather.data.remote.tomorrow

import com.google.gson.annotations.SerializedName

data class TomorrowForecastResponse(
    @SerializedName("timelines")
    val timelines: TomorrowTimelines? = null,
    @SerializedName("location")
    val location: TomorrowLocation? = null
)

data class TomorrowTimelines(
    @SerializedName("minutely")
    val minutely: List<TomorrowInterval>? = null,
    @SerializedName("hourly")
    val hourly: List<TomorrowInterval>? = null,
    @SerializedName("daily")
    val daily: List<TomorrowInterval>? = null
)

data class TomorrowInterval(
    @SerializedName("time")
    val time: String? = null,
    @SerializedName("values")
    val values: TomorrowValues? = null
)

data class TomorrowValues(
    @SerializedName("temperature")
    val temperature: Double? = null,
    @SerializedName("temperatureMax")
    val temperatureMax: Double? = null,
    @SerializedName("temperatureMin")
    val temperatureMin: Double? = null,
    @SerializedName("humidity")
    val humidity: Double? = null,
    @SerializedName("windSpeed")
    val windSpeed: Double? = null,
    @SerializedName("precipitationProbability")
    val precipitationProbability: Double? = null,
    @SerializedName("weatherCode")
    val weatherCode: Int? = null,
    @SerializedName("uvIndex")
    val uvIndex: Double? = null,
    @SerializedName("visibility")
    val visibility: Double? = null
)

data class TomorrowLocation(
    @SerializedName("lat")
    val lat: Double? = null,
    @SerializedName("lon")
    val lon: Double? = null,
    @SerializedName("name")
    val name: String? = null
)
