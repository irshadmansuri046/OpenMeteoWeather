package com.example.openmeteoweather.domain.accuracy

import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.ProviderType

/**
 * Placeholder interface for future validation and ML-based tracking of weather forecast accuracy.
 */
interface ForecastAccuracyTracker {
    suspend fun trackAccuracy(
        provider: ProviderType,
        predicted: WeatherInfo,
        actual: WeatherInfo
    )
    
    suspend fun getProviderRankings(): Map<ProviderType, Double>
}
