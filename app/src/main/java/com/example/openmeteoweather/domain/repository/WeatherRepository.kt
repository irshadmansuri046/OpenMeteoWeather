package com.example.openmeteoweather.domain.repository

import com.example.openmeteoweather.domain.model.LocationInfo
import com.example.openmeteoweather.domain.model.WeatherInfo
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun getWeatherData(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean
    ): WeatherInfo

    suspend fun searchLocation(query: String): List<LocationInfo>

    fun getRecentSearches(): Flow<List<LocationInfo>>

    suspend fun saveRecentSearch(location: LocationInfo)

    suspend fun deleteRecentSearch(location: LocationInfo)

    suspend fun clearRecentSearches()
}
