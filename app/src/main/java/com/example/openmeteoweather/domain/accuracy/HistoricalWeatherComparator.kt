package com.example.openmeteoweather.domain.accuracy

import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.ProviderType

/**
 * Placeholder interface for future time-series analyses and historical weather comparisons.
 */
interface HistoricalWeatherComparator {
    suspend fun compareHistorical(
        latitude: Double,
        longitude: Double,
        provider: ProviderType,
        days: Int
    ): Double
}
