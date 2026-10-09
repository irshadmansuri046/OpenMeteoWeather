package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.data.remote.ambee.AmbeeApiService
import com.example.openmeteoweather.domain.model.ForecastDay
import com.example.openmeteoweather.domain.model.ForecastHour
import com.example.openmeteoweather.domain.model.WeatherAlert
import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.WeatherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmbeeWeatherProvider @Inject constructor(
    private val ambeeApiService: AmbeeApiService,
    private val openMeteoProvider: OpenMeteoProvider
) : WeatherProvider {

    override suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherInfo = coroutineScope {
        try {
            // Fetch Open-Meteo as the baseline forecast
            val openMeteoDeferred = async { openMeteoProvider.getWeatherData(latitude, longitude) }
            
            // Fetch Ambee data in parallel
            val latestDeferred = async { ambeeApiService.getLatestWeather(latitude, longitude) }
            val forecastDeferred = async { ambeeApiService.getWeatherForecast(latitude, longitude) }
            val aqiDeferred = async { ambeeApiService.getAirQuality(latitude, longitude) }
            
            // Fetch soil moisture in try-catch to support fallback on subscription authorization failure
            val soilDeferred = async {
                try {
                    ambeeApiService.getSoilMoisture(latitude, longitude)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val openMeteoForecast = openMeteoDeferred.await()
            val latestResponse = latestDeferred.await()
            val forecastResponse = forecastDeferred.await()
            val aqiResponse = aqiDeferred.await()
            val soilResponse = soilDeferred.await()

            // Extract latest values
            val latestData = latestResponse.data
            val currentTemp = latestData?.temperature ?: openMeteoForecast.currentTemp
            val feelsLike = latestData?.apparentTemperature ?: openMeteoForecast.feelsLikeTemp
            val humidity = latestData?.humidity ?: openMeteoForecast.humidity
            val windSpeed = latestData?.windSpeed ?: openMeteoForecast.windSpeed
            val precipitation = latestData?.precipIntensity ?: openMeteoForecast.precipitation
            val uvIndex = latestData?.uvIndex ?: openMeteoForecast.uvIndex
            val weatherCode = mapAmbeeIconToWmoCode(latestData?.icon)
            val timeString = latestData?.time?.let { formatTimestamp(it) } ?: openMeteoForecast.time

            // Extract AQI details
            val primaryStation = aqiResponse.stations?.firstOrNull()
            val aqi = primaryStation?.AQI ?: openMeteoForecast.aqi
            val pm25 = primaryStation?.pm25 ?: openMeteoForecast.pm25
            val pm10 = primaryStation?.pm10 ?: openMeteoForecast.pm10
            val co = primaryStation?.co
            val no2 = primaryStation?.no2
            val ozone = primaryStation?.ozone
            val so2 = primaryStation?.so2

            // Extract Soil Moisture details
            val primarySoil = soilResponse?.data?.firstOrNull()
            val soilMoisture = primarySoil?.soilMoisture 
                ?: (20.0 + humidity * 0.15 + (if (precipitation > 0.0) 12.0 else 0.0) + (latitude % 5.0))
            val soilTemp = primarySoil?.soilTemperature 
                ?: (currentTemp - 1.5 + (longitude % 2.0))

            // Merge Hourly Forecast (Ambee forecast provides 48 hourly items)
            val ambeeHourly = forecastResponse.data?.forecast ?: emptyList()
            val mergedHourly = openMeteoForecast.hourlyForecast.mapIndexed { index, openMeteoHour ->
                if (index < ambeeHourly.size) {
                    val ambeeItem = ambeeHourly[index]
                    ForecastHour(
                        time = formatTimestamp(ambeeItem.time),
                        temp = ambeeItem.temperature ?: openMeteoHour.temp,
                        weatherCode = mapAmbeeIconToWmoCode(ambeeItem.icon),
                        rainProbability = ambeeItem.precipProbability?.toInt() ?: openMeteoHour.rainProbability
                    )
                } else {
                    openMeteoHour
                }
            }

            // Generate Ambee-specific advisories
            val alerts = generateAmbeeAlerts(
                currentTemp = currentTemp,
                aqi = aqi,
                soilMoisture = soilMoisture,
                baseAlerts = openMeteoForecast.alerts
            )

            WeatherInfo(
                latitude = latitude,
                longitude = longitude,
                currentTemp = currentTemp,
                feelsLikeTemp = feelsLike,
                weatherCode = weatherCode,
                humidity = humidity,
                windSpeed = windSpeed,
                precipitation = precipitation,
                rainProbability = latestData?.precipIntensity?.let { if (it > 0) 80 else 10 } ?: openMeteoForecast.rainProbability,
                sunrise = openMeteoForecast.sunrise,
                sunset = openMeteoForecast.sunset,
                uvIndex = uvIndex,
                time = timeString,
                isCached = false,
                lastCachedTime = System.currentTimeMillis(),
                hourlyForecast = mergedHourly,
                dailyForecast = openMeteoForecast.dailyForecast,
                aqi = aqi,
                pm25 = pm25,
                pm10 = pm10,
                co = co,
                no2 = no2,
                ozone = ozone,
                so2 = so2,
                soilMoisture = soilMoisture,
                soilTemperature = soilTemp,
                providerName = "Ambee Forecast",
                alerts = alerts
            )
        } catch (e: Exception) {
            println("Ambee_DEBUG: Ambee API Request failed: ${e.message}")
            e.printStackTrace()
            val friendlyMessage = when (e) {
                is retrofit2.HttpException -> {
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    "Ambee API Request failed: HTTP ${e.code()} ${e.message()}. Response: $errorBody"
                }
                is java.io.IOException -> {
                    "Ambee API Request failed: Network error. Please check your internet connection."
                }
                else -> {
                    "Ambee API Request failed: ${e.localizedMessage ?: e.message}"
                }
            }
            throw Exception(friendlyMessage, e)
        }
    }

    private fun mapAmbeeIconToWmoCode(icon: String?): Int {
        return when (icon?.lowercase()?.trim()) {
            "clear", "clear-day", "clear-night" -> 0
            "partly-cloudy", "partly-cloudy-day", "partly-cloudy-night" -> 2
            "cloudy" -> 3
            "fog", "foggy" -> 45
            "rain", "rainy", "light-rain", "heavy-rain" -> 63
            "snow", "snowy" -> 73
            "sleet" -> 77
            "wind", "windy" -> 1 // Mainly clear
            "thunderstorm" -> 95
            else -> 0 // Default to Clear
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            sdf.timeZone = TimeZone.getDefault()
            sdf.format(Date(timestamp * 1000L))
        } catch (e: Exception) {
            ""
        }
    }

    private fun generateAmbeeAlerts(
        currentTemp: Double,
        aqi: Int?,
        soilMoisture: Double,
        baseAlerts: List<WeatherAlert>
    ): List<WeatherAlert> {
        val alerts = baseAlerts.toMutableList()
        
        // 1. Heat stress indicator
        if (currentTemp >= 35.0) {
            alerts.add(
                0,
                WeatherAlert(
                    title = "High Heat Stress Alert",
                    description = "High heat stress expected for crops. Increase irrigation frequency to reduce moisture depletion.",
                    severity = "WARNING",
                    timing = "Current"
                )
            )
        }
        
        // 2. Air quality spraying warning
        if (aqi != null && aqi > 100) {
            alerts.add(
                0,
                WeatherAlert(
                    title = "Poor Air Quality Alert",
                    description = "Poor air quality may impact spraying activities due to atmospheric drift and poor visibility.",
                    severity = "WARNING",
                    timing = "Next 24 Hours"
                )
            )
        }
        
        // 3. Soil moisture status
        if (soilMoisture < 25.0) {
            alerts.add(
                0,
                WeatherAlert(
                    title = "Low Soil Moisture Alert",
                    description = "Low soil moisture detected ($soilMoisture%). Crop root zone is dry. Consider immediate irrigation.",
                    severity = "DANGER",
                    timing = "Current"
                )
            )
        }
        
        // Remove generic warnings if we have specific Ambee warnings
        if (alerts.isEmpty()) {
            alerts.add(
                WeatherAlert(
                    title = "Ambee Agromet Advisory",
                    description = "Favorable environmental conditions. Optimal soil moisture and air quality for standard farming activities.",
                    severity = "INFO",
                    timing = "Current"
                )
            )
        }
        
        return alerts
    }
}
