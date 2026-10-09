package com.example.openmeteoweather.domain.model

data class LocationInfo(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?, // State
    val timezone: String?
)
