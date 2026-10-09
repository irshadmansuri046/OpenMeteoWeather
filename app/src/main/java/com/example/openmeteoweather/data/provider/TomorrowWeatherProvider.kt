package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.data.remote.tomorrow.TomorrowApiService
import com.example.openmeteoweather.domain.model.ForecastDay
import com.example.openmeteoweather.domain.model.ForecastHour
import com.example.openmeteoweather.domain.model.WeatherAlert
import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.WeatherProvider
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TomorrowWeatherProvider @Inject constructor(
    private val tomorrowApiService: TomorrowApiService
) : WeatherProvider {

    override suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherInfo {
        return try {
            val locationQuery = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
            val response = tomorrowApiService.getForecast(locationQuery)
            
            val timelines = response.timelines ?: throw Exception("Tomorrow.io timelines is null")
            val hourlyList = timelines.hourly ?: emptyList()
            val dailyList = timelines.daily ?: emptyList()
            
            // Map Hourly Forecast
            val domainHourly = hourlyList.map { hour ->
                ForecastHour(
                    time = hour.time ?: "",
                    temp = hour.values?.temperature ?: 0.0,
                    weatherCode = mapTomorrowCodeToWmo(hour.values?.weatherCode),
                    rainProbability = hour.values?.precipitationProbability?.toInt() ?: 0
                )
            }
            
            // Map Daily Forecast
            val domainDaily = dailyList.map { day ->
                ForecastDay(
                    date = day.time?.split("T")?.firstOrNull() ?: "",
                    tempMin = day.values?.temperatureMin ?: 0.0,
                    tempMax = day.values?.temperatureMax ?: 0.0,
                    weatherCode = mapTomorrowCodeToWmo(day.values?.weatherCode),
                    sunrise = day.values?.uvIndex?.toString() ?: "", // Mocking sunrise as we don't have it direct
                    sunset = "",
                    uvIndex = day.values?.uvIndex ?: 0.0
                )
            }
            
            // Extract latest/current values from the first hourly item
            val currentHour = hourlyList.firstOrNull()
            val currentTemp = currentHour?.values?.temperature ?: 0.0
            val feelsLike = currentHour?.values?.temperature ?: 0.0 // Tomorrow.io returns temperature, we can use it as apparent temp
            val humidity = currentHour?.values?.humidity ?: 0.0
            val windSpeed = currentHour?.values?.windSpeed ?: 0.0
            val rainProb = currentHour?.values?.precipitationProbability?.toInt() ?: 0
            val uv = currentHour?.values?.uvIndex ?: 0.0
            val weatherCode = mapTomorrowCodeToWmo(currentHour?.values?.weatherCode)
            val timeString = currentHour?.time ?: ""
            
            val alerts = generateTomorrowAlerts(
                currentTemp = currentTemp,
                rainProbability = rainProb,
                windSpeed = windSpeed,
                uvIndex = uv
            )

            WeatherInfo(
                latitude = latitude,
                longitude = longitude,
                currentTemp = currentTemp,
                feelsLikeTemp = feelsLike,
                weatherCode = weatherCode,
                humidity = humidity,
                windSpeed = windSpeed,
                precipitation = 0.0,
                rainProbability = rainProb,
                sunrise = "",
                sunset = "",
                uvIndex = uv,
                time = timeString,
                isCached = false,
                lastCachedTime = System.currentTimeMillis(),
                hourlyForecast = domainHourly,
                dailyForecast = domainDaily,
                aqi = null,
                pm25 = null,
                pm10 = null,
                providerName = "Tomorrow.io",
                alerts = alerts
            )
        } catch (e: Exception) {
            println("Tomorrow_DEBUG: Tomorrow.io API Request failed: ${e.message}")
            e.printStackTrace()
            val friendlyMessage = when (e) {
                is retrofit2.HttpException -> {
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    "Tomorrow.io API Request failed: HTTP ${e.code()} ${e.message()}. Response: $errorBody"
                }
                is java.io.IOException -> {
                    "Tomorrow.io API Request failed: Network error. Please check your internet connection."
                }
                else -> {
                    "Tomorrow.io API Request failed: ${e.localizedMessage ?: e.message}"
                }
            }
            throw Exception(friendlyMessage, e)
        }
    }

    private fun mapTomorrowCodeToWmo(code: Int?): Int {
        return when (code) {
            1000 -> 0 // Clear Sky
            1100 -> 1 // Mainly Clear
            1101 -> 2 // Partly Cloudy
            1102, 1001 -> 3 // Overcast
            2000, 2100 -> 45 // Foggy
            4000, 4200 -> 51 // Light Drizzle
            4001, 4201 -> 63 // Moderate Rain
            5000, 5001, 5100, 5101 -> 73 // Moderate Snowfall
            6000, 6001, 6200, 6201, 7000, 7101, 7102 -> 66 // Light Freezing Rain
            8000 -> 95 // Thunderstorm
            else -> 0 // Clear Sky
        }
    }

    private fun generateTomorrowAlerts(
        currentTemp: Double,
        rainProbability: Int,
        windSpeed: Double,
        uvIndex: Double
    ): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()
        
        // 1. Rain Warning
        if (rainProbability > 80) {
            alerts.add(
                WeatherAlert(
                    title = "Heavy Rainfall Expected",
                    description = "High probability of rain ($rainProbability%) in the next hours. Protect open crops.",
                    severity = "WARNING",
                    timing = "Next 6 Hours"
                )
            )
        }
        
        // 2. Heat Stress Warning
        if (currentTemp >= 35.0) {
            alerts.add(
                WeatherAlert(
                    title = "Heat Stress Alert",
                    description = "High heat stress risk for crops tomorrow afternoon. Increase irrigation frequency to reduce moisture depletion.",
                    severity = "WARNING",
                    timing = "Next 24 Hours"
                )
            )
        }
        
        // 3. Wind Warning
        if (windSpeed >= 25.0) {
            alerts.add(
                WeatherAlert(
                    title = "High Wind Warning",
                    description = "High wind speed detected ($windSpeed km/h). Avoid pesticide spraying.",
                    severity = "WARNING",
                    timing = "Next 12 Hours"
                )
            )
        }
        
        // 4. Spraying safety
        if (rainProbability > 50 || windSpeed >= 25.0) {
            alerts.add(
                WeatherAlert(
                    title = "Spraying Advisory",
                    description = "Weather conditions unfavorable for spraying due to potential drift or rain wash-off.",
                    severity = "INFO",
                    timing = "Current"
                )
            )
        }
        
        if (alerts.isEmpty()) {
            alerts.add(
                WeatherAlert(
                    title = "Tomorrow.io Agromet Advisory",
                    description = "Favorable environmental conditions. Optimal temperature, wind speed, and moisture for farming activities.",
                    severity = "INFO",
                    timing = "Current"
                )
            )
        }
        
        return alerts
    }
}
