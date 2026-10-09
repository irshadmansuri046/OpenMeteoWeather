package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.domain.model.ForecastDay
import com.example.openmeteoweather.domain.model.ForecastHour
import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.ProviderType

object ForecastConsensusCalculator {

    /**
     * Combines multiple WeatherInfo outputs into a single consensus forecast using weights.
     */
    fun calculateConsensus(
        successfulForecasts: Map<ProviderType, WeatherInfo>,
        weights: Map<ProviderType, Double>
    ): WeatherInfo {
        if (successfulForecasts.isEmpty()) {
            throw IllegalArgumentException("Cannot calculate consensus with zero forecasts")
        }

        // Find the provider with the highest weight for string/enum mappings
        val highestWeightProvider = successfulForecasts.keys.maxByOrNull { weights[it] ?: 0.0 }
            ?: successfulForecasts.keys.first()
        val primaryForecast = successfulForecasts[highestWeightProvider]!!

        var tempSum = 0.0
        var feelsLikeSum = 0.0
        var humiditySum = 0.0
        var windSpeedSum = 0.0
        var rainProbSum = 0.0
        var precipitationSum = 0.0
        var uvIndexSum = 0.0

        for ((type, weather) in successfulForecasts) {
            val weight = weights[type] ?: 0.0
            tempSum += weather.currentTemp * weight
            feelsLikeSum += weather.feelsLikeTemp * weight
            humiditySum += weather.humidity * weight
            windSpeedSum += weather.windSpeed * weight
            rainProbSum += weather.rainProbability * weight
            precipitationSum += weather.precipitation * weight
            uvIndexSum += weather.uvIndex * weight
        }

        // Compute hourly forecast consensus
        // Group hourly items across all providers by their time string
        val allHourlyItems = successfulForecasts.flatMap { (type, weather) ->
            weather.hourlyForecast.map { hour -> type to hour }
        }
        val hourlyByTime = allHourlyItems.groupBy { it.second.time }
        val consensusHourly = hourlyByTime.map { (time, items) ->
            var hTempSum = 0.0
            var hRainProbSum = 0.0
            var weightSum = 0.0

            // Filter items that belong to active providers
            items.forEach { (type, hour) ->
                val weight = weights[type] ?: 0.0
                hTempSum += hour.temp * weight
                hRainProbSum += hour.rainProbability * weight
                weightSum += weight
            }

            val finalTemp = if (weightSum > 0.0) hTempSum / weightSum else 0.0
            val finalRainProb = if (weightSum > 0.0) (hRainProbSum / weightSum).toInt() else 0

            // Get weather code of the highest-weight provider for this hour
            val primaryHour = items.firstOrNull { it.first == highestWeightProvider }?.second
                ?: items.firstOrNull()?.second
            val finalCode = primaryHour?.weatherCode ?: 0

            ForecastHour(
                time = time,
                temp = finalTemp,
                weatherCode = finalCode,
                rainProbability = finalRainProb
            )
        }.sortedBy { it.time }

        // Compute daily forecast consensus
        val allDailyItems = successfulForecasts.flatMap { (type, weather) ->
            weather.dailyForecast.map { day -> type to day }
        }
        val dailyByDate = allDailyItems.groupBy { it.second.date }
        val consensusDaily = dailyByDate.map { (date, items) ->
            var dMinSum = 0.0
            var dMaxSum = 0.0
            var dUvSum = 0.0
            var weightSum = 0.0

            items.forEach { (type, day) ->
                val weight = weights[type] ?: 0.0
                dMinSum += day.tempMin * weight
                dMaxSum += day.tempMax * weight
                dUvSum += day.uvIndex * weight
                weightSum += weight
            }

            val finalMin = if (weightSum > 0.0) dMinSum / weightSum else 0.0
            val finalMax = if (weightSum > 0.0) dMaxSum / weightSum else 0.0
            val finalUv = if (weightSum > 0.0) dUvSum / weightSum else 0.0

            val primaryDay = items.firstOrNull { it.first == highestWeightProvider }?.second
                ?: items.firstOrNull()?.second
            val finalCode = primaryDay?.weatherCode ?: 0

            ForecastDay(
                date = date,
                tempMin = finalMin,
                tempMax = finalMax,
                weatherCode = finalCode,
                sunrise = primaryDay?.sunrise ?: "",
                sunset = primaryDay?.sunset ?: "",
                uvIndex = finalUv
            )
        }.sortedBy { it.date }

        return WeatherInfo(
            latitude = primaryForecast.latitude,
            longitude = primaryForecast.longitude,
            currentTemp = tempSum,
            feelsLikeTemp = feelsLikeSum,
            weatherCode = primaryForecast.weatherCode,
            humidity = humiditySum,
            windSpeed = windSpeedSum,
            precipitation = precipitationSum,
            rainProbability = rainProbSum.toInt(),
            sunrise = primaryForecast.sunrise,
            sunset = primaryForecast.sunset,
            uvIndex = uvIndexSum,
            time = primaryForecast.time,
            isCached = false,
            lastCachedTime = System.currentTimeMillis(),
            hourlyForecast = consensusHourly,
            dailyForecast = consensusDaily,
            aqi = primaryForecast.aqi,
            pm25 = primaryForecast.pm25,
            pm10 = primaryForecast.pm10,
            soilMoisture = primaryForecast.soilMoisture,
            soilTemperature = primaryForecast.soilTemperature,
            providerName = "Combined Weather",
            alerts = primaryForecast.alerts
        )
    }
}
