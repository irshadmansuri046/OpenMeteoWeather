package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.domain.model.ProviderForecastBreakdown
import com.example.openmeteoweather.domain.model.WeatherAlert
import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.ProviderType
import com.example.openmeteoweather.domain.provider.WeatherProvider
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CombinedWeatherProvider @Inject constructor(
    private val ensembleForecastEngine: EnsembleForecastEngine
) : WeatherProvider {

    private val gson = Gson()

    override suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherInfo {
        val results = ensembleForecastEngine.fetchAllProviders(latitude, longitude)

        // Filter successful forecasts
        val successfulForecasts = results.filterValues { it.isSuccess }
            .mapValues { it.value.getOrThrow() }

        if (successfulForecasts.isEmpty()) {
            val errorsSummary = results.entries.joinToString("; ") { "${it.key.name}: ${it.value.exceptionOrNull()?.message}" }
            throw Exception("Combined Weather calculation failed: All constituent providers failed to fetch. Errors: $errorsSummary")
        }

        // Get normalized weights based on which providers succeeded
        val normalizedWeights = WeatherWeightManager.getNormalizedWeights(successfulForecasts.keys.toList())

        // Calculate consensus
        val consensus = ForecastConsensusCalculator.calculateConsensus(successfulForecasts, normalizedWeights)

        // Calculate confidence
        val confidence = ConfidenceScoreCalculator.calculateConfidence(consensus, successfulForecasts, normalizedWeights)

        // Map breakdowns for comparison card
        val breakdowns = listOf(
            ProviderType.TOMORROW_IO,
            ProviderType.AMBEE,
            ProviderType.OPEN_WEATHER,
            ProviderType.SWAN_WEATHER,
            ProviderType.OPEN_METEO
        ).map { type ->
            val result = results[type]
            val isSuccess = result != null && result.isSuccess
            val weather = result?.getOrNull()

            ProviderForecastBreakdown(
                providerName = when (type) {
                    ProviderType.OPEN_METEO -> "Open-Meteo"
                    ProviderType.AMBEE -> "Ambee"
                    ProviderType.TOMORROW_IO -> "Tomorrow.io"
                    ProviderType.OPEN_WEATHER -> "OpenWeather"
                    ProviderType.SWAN_WEATHER -> "SwanWeather"
                    else -> type.name
                },
                rainProbability = weather?.rainProbability ?: 0,
                temp = weather?.currentTemp ?: 0.0,
                windSpeed = weather?.windSpeed ?: 0.0,
                humidity = weather?.humidity ?: 0.0,
                isSuccess = isSuccess
            )
        }

        // Build combined alerts and agricultural advisories
        val advisories = generateConsensusAdvisories(consensus, successfulForecasts, confidence)

        return consensus.copy(
            providerName = "Combined Weather",
            confidenceScore = confidence.confidenceScore,
            confidenceLevel = confidence.confidenceLevel,
            agreementPercentage = confidence.agreementPercentage,
            providerBreakdownsJson = gson.toJson(breakdowns),
            alerts = advisories
        )
    }

    private fun generateConsensusAdvisories(
        consensus: WeatherInfo,
        successfulForecasts: Map<ProviderType, WeatherInfo>,
        confidence: ConfidenceScoreCalculator.ConfidenceResult
    ): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()

        // 1. Heatwave consensus alert
        if (consensus.currentTemp >= 35.0) {
            val level = confidence.confidenceLevel
            alerts.add(
                WeatherAlert(
                    title = "Consensus Heatwave Advisory",
                    description = "High confidence heatwave prediction ($level Confidence). Consensus temp is ${consensus.currentTemp.toInt()}°C. Increase irrigation.",
                    severity = "WARNING",
                    timing = "Next 24 Hours"
                )
            )
        }

        // 2. Rainfall consensus alert
        val highRainProviders = successfulForecasts.values.count { it.rainProbability > 60 }
        if (consensus.rainProbability > 60) {
            if (highRainProviders == successfulForecasts.size) {
                alerts.add(
                    WeatherAlert(
                        title = "Consensus Rain Advisory",
                        description = "All providers indicate strong rainfall probability. Delay pesticide spraying and irrigation.",
                        severity = "WARNING",
                        timing = "Next 6 Hours"
                    )
                )
            } else {
                alerts.add(
                    WeatherAlert(
                        title = "Consensus Rain Advisory",
                        description = "Rain expected (${consensus.rainProbability}% probability). Delay irrigation.",
                        severity = "WARNING",
                        timing = "Next 6 Hours"
                    )
                )
            }
        }

        // 3. Disagreement alert
        val rainProbabilities = successfulForecasts.values.map { it.rainProbability }
        val maxRainProb = rainProbabilities.maxOrNull() ?: 0
        val minRainProb = rainProbabilities.minOrNull() ?: 0
        if (maxRainProb - minRainProb > 40) {
            alerts.add(
                WeatherAlert(
                    title = "Forecast Disagreement Detected",
                    description = "Forecast disagreement detected for rainfall tomorrow. Spread is ${minRainProb}% to ${maxRainProb}%. Keep monitoring.",
                    severity = "INFO",
                    timing = "Next 24 Hours"
                )
            )
        }

        // 4. Wind consensus alert
        if (consensus.windSpeed >= 20.0) {
            alerts.add(
                WeatherAlert(
                    title = "Consensus Wind Advisory",
                    description = "Strong wind conditions detected (${consensus.windSpeed.toInt()} km/h). Avoid pesticide spraying.",
                    severity = "WARNING",
                    timing = "Next 12 Hours"
                )
            )
        }

        // 5. Humidity consensus alert
        if (consensus.humidity > 85.0) {
            alerts.add(
                WeatherAlert(
                    title = "Consensus Fungal Alert",
                    description = "High humidity (${consensus.humidity.toInt()}%) may increase fungal disease risk.",
                    severity = "WARNING",
                    timing = "Current"
                )
            )
        }

        if (alerts.isEmpty()) {
            alerts.add(
                WeatherAlert(
                    title = "Combined Agromet Advisory",
                    description = "Optimal ensemble conditions. High forecast confidence for normal farming schedules.",
                    severity = "INFO",
                    timing = "Current"
                )
            )
        }

        return alerts
    }
}
