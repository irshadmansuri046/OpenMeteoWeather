package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.domain.provider.ProviderType

object WeatherWeightManager {
    
    // Default weights for ensemble calculation:
    // Tomorrow.io: 30%, SwanWeather: 20%, Ambee: 20%, OpenWeather: 15%, Open-Meteo: 15%
    private val defaultWeights = mapOf(
        ProviderType.TOMORROW_IO to 0.30,
        ProviderType.SWAN_WEATHER to 0.20,
        ProviderType.AMBEE to 0.20,
        ProviderType.OPEN_WEATHER to 0.15,
        ProviderType.OPEN_METEO to 0.15
    )

    fun getWeights(): Map<ProviderType, Double> = defaultWeights

    /**
     * Normalizes weights based on the providers that successfully fetched data.
     * Redistributes the weight of failed providers proportionally.
     */
    fun getNormalizedWeights(successfulProviders: List<ProviderType>): Map<ProviderType, Double> {
        val activeWeights = defaultWeights.filterKeys { it in successfulProviders }
        val sum = activeWeights.values.sum()
        
        if (sum <= 0.0) {
            // Fallback to equal weights if no predefined weight matches
            if (successfulProviders.isEmpty()) return emptyMap()
            val equalWeight = 1.0 / successfulProviders.size
            return successfulProviders.associateWith { equalWeight }
        }
        
        return activeWeights.mapValues { it.value / sum }
    }
}
