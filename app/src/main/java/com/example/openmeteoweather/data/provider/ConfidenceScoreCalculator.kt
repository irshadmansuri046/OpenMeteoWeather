package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.ProviderType
import kotlin.math.abs

object ConfidenceScoreCalculator {

    data class ConfidenceResult(
        val agreementPercentage: Double,
        val confidenceScore: Double,
        val confidenceLevel: String
    )

    /**
     * Computes the variance and agreement percentage of the forecasts compared to the consensus.
     */
    fun calculateConfidence(
        consensus: WeatherInfo,
        successfulForecasts: Map<ProviderType, WeatherInfo>,
        weights: Map<ProviderType, Double>
    ): ConfidenceResult {
        if (successfulForecasts.isEmpty()) {
            return ConfidenceResult(0.0, 0.0, "Low")
        }

        // If only 1 provider succeeded, agreement is 100% but confidence level is capped to Medium
        if (successfulForecasts.size == 1) {
            return ConfidenceResult(100.0, 60.0, "Medium")
        }

        var totalWeightedAgreement = 0.0
        var totalWeight = 0.0

        for ((type, weather) in successfulForecasts) {
            val weight = weights[type] ?: 0.0
            
            // Calculate agreement scores per variable (clamped between 0.0 and 1.0)
            val tempDiff = abs(weather.currentTemp - consensus.currentTemp)
            val tempAgreement = (1.0 - (tempDiff / 8.0)).coerceIn(0.0, 1.0)

            val rainDiff = abs(weather.rainProbability - consensus.rainProbability)
            val rainAgreement = (1.0 - (rainDiff / 80.0)).coerceIn(0.0, 1.0)

            val windDiff = abs(weather.windSpeed - consensus.windSpeed)
            val windAgreement = (1.0 - (windDiff / 20.0)).coerceIn(0.0, 1.0)

            val humDiff = abs(weather.humidity - consensus.humidity)
            val humAgreement = (1.0 - (humDiff / 80.0)).coerceIn(0.0, 1.0)

            val providerAgreement = (tempAgreement + rainAgreement + windAgreement + humAgreement) / 4.0
            
            totalWeightedAgreement += providerAgreement * weight
            totalWeight += weight
        }

        val agreementFraction = if (totalWeight > 0.0) totalWeightedAgreement / totalWeight else 0.0
        val agreementPercentage = agreementFraction * 100.0

        // Adjust confidence score by the count of active providers
        val providerCountFactor = when (successfulForecasts.size) {
            4 -> 1.0
            3 -> 0.9
            2 -> 0.8
            else -> 0.6
        }
        val confidenceScore = agreementPercentage * providerCountFactor

        val confidenceLevel = when {
            confidenceScore > 85.0 -> "Very High"
            confidenceScore >= 70.0 -> "High"
            confidenceScore >= 50.0 -> "Medium"
            else -> "Low"
        }

        return ConfidenceResult(
            agreementPercentage = agreementPercentage,
            confidenceScore = confidenceScore,
            confidenceLevel = confidenceLevel
        )
    }
}
