package com.example.openmeteoweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey
    val id: Int, // Open-Meteo geocoding result ID is unique, so we can use it as the PK
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?,
    val admin1: String?, // state
    val timezone: String?,
    val timestamp: Long // to sort by recent
)
