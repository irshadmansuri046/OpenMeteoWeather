package com.example.openmeteoweather

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class Home(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val cityName: String? = null
) : NavKey

@Serializable
data object Search : NavKey
