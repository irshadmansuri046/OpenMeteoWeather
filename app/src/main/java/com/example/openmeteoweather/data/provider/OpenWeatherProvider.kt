package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.data.remote.openweather.OpenWeatherApiService
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenWeatherProvider @Inject constructor(
    private val openWeatherApiService: OpenWeatherApiService
) : WeatherProvider {

    override suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherInfo {
        return try {
            // Try fetching from One Call 3.0 API first
            fetchFromOneCall30(latitude, longitude)
        } catch (e: Exception) {
            // Log the One Call 3.0 failure and trigger the 2.5 fallback APIs
            println("OpenWeather_DEBUG: One Call 3.0 failed, trying 2.5 fallback APIs: ${e.message}")
            try {
                fetchFromWeatherAndForecast25(latitude, longitude)
            } catch (fallbackEx: Exception) {
                println("OpenWeather_DEBUG: 2.5 Fallback APIs failed: ${fallbackEx.message}")
                fallbackEx.printStackTrace()
                // Propagate the original error (so we show detailed status/body info to the user)
                val friendlyMessage = when (e) {
                    is retrofit2.HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string() ?: ""
                        "OpenWeather API Request failed: HTTP ${e.code()} ${e.message()}. Response: $errorBody"
                    }
                    is java.io.IOException -> {
                        "OpenWeather API Request failed: Network error. Please check your internet connection."
                    }
                    else -> {
                        "OpenWeather API Request failed: ${e.localizedMessage ?: e.message}"
                    }
                }
                throw Exception(friendlyMessage, e)
            }
        }
    }

    private suspend fun fetchFromOneCall30(latitude: Double, longitude: Double): WeatherInfo {
        val response = openWeatherApiService.getOneCallForecast(lat = latitude, lon = longitude)
        
        val current = response.current ?: throw Exception("OpenWeather current weather data is missing")
        val hourlyList = response.hourly ?: emptyList()
        val dailyList = response.daily ?: emptyList()

        // Format Unix Timestamps
        val currentIsoTime = formatEpochToIsoString(current.dt)
        val currentSunrise = current.sunrise?.let { formatEpochToIsoString(it) } ?: ""
        val currentSunset = current.sunset?.let { formatEpochToIsoString(it) } ?: ""
        
        // Map Hourly Forecast
        val domainHourly = hourlyList.map { hour ->
            ForecastHour(
                time = formatEpochToIsoString(hour.dt),
                temp = hour.temp,
                weatherCode = mapOpenWeatherCodeToWmo(hour.weather?.firstOrNull()?.id),
                rainProbability = ((hour.pop ?: 0.0) * 100).toInt()
            )
        }

        // Map Daily Forecast
        val domainDaily = dailyList.map { day ->
            ForecastDay(
                date = formatEpochToDateString(day.dt),
                tempMin = day.temp?.min ?: 0.0,
                tempMax = day.temp?.max ?: 0.0,
                weatherCode = mapOpenWeatherCodeToWmo(day.weather?.firstOrNull()?.id),
                sunrise = day.sunrise?.let { formatEpochToIsoString(it) } ?: "",
                sunset = day.sunset?.let { formatEpochToIsoString(it) } ?: "",
                uvIndex = day.uvi ?: 0.0
            )
        }

        // Convert wind speed from m/s to km/h (1 m/s = 3.6 km/h)
        val windSpeedKmh = current.windSpeed * 3.6
        val currentRainProb = ((hourlyList.firstOrNull()?.pop ?: 0.0) * 100).toInt()
        val weatherCode = mapOpenWeatherCodeToWmo(current.weather?.firstOrNull()?.id)

        // Map standard government/severe alerts
        val baseAlerts = response.alerts?.map { alertDto ->
            WeatherAlert(
                title = alertDto.event ?: "Weather Alert",
                description = alertDto.description ?: "",
                severity = "WARNING",
                timing = "${formatEpochToIsoString(alertDto.start)} - ${formatEpochToIsoString(alertDto.end)}"
            )
        } ?: emptyList()

        // Generate Agromet Advisories
        val alerts = generateOpenWeatherAlerts(
            currentTemp = current.temp,
            rainProbability = currentRainProb,
            windSpeed = windSpeedKmh,
            humidity = current.humidity,
            uvIndex = current.uvi ?: 0.0,
            baseAlerts = baseAlerts
        )

        return WeatherInfo(
            latitude = latitude,
            longitude = longitude,
            currentTemp = current.temp,
            feelsLikeTemp = current.feelsLike,
            weatherCode = weatherCode,
            humidity = current.humidity,
            windSpeed = windSpeedKmh,
            precipitation = 0.0,
            rainProbability = currentRainProb,
            sunrise = currentSunrise,
            sunset = currentSunset,
            uvIndex = current.uvi ?: 0.0,
            time = currentIsoTime,
            isCached = false,
            lastCachedTime = System.currentTimeMillis(),
            hourlyForecast = domainHourly,
            dailyForecast = domainDaily,
            aqi = null,
            pm25 = null,
            pm10 = null,
            providerName = "OpenWeather",
            alerts = alerts
        )
    }

    private suspend fun fetchFromWeatherAndForecast25(latitude: Double, longitude: Double): WeatherInfo = coroutineScope {
        val currentDeferred = async { openWeatherApiService.getCurrentWeather(lat = latitude, lon = longitude) }
        val forecastDeferred = async { openWeatherApiService.getForecast25(lat = latitude, lon = longitude) }

        val current = currentDeferred.await()
        val forecast = forecastDeferred.await()

        val currentTemp = current.main?.temp ?: 0.0
        val feelsLike = current.main?.feelsLike ?: 0.0
        val humidity = current.main?.humidity ?: 0.0
        val windSpeedKmh = (current.wind?.speed ?: 0.0) * 3.6
        val weatherCode = mapOpenWeatherCodeToWmo(current.weather?.firstOrNull()?.id)
        val currentIsoTime = formatEpochToIsoString(current.dt)
        val currentSunrise = current.sys?.sunrise?.let { formatEpochToIsoString(it) } ?: ""
        val currentSunset = current.sys?.sunset?.let { formatEpochToIsoString(it) } ?: ""

        val forecastList = forecast.list ?: emptyList()

        // Map Hourly Forecast (every 3 hours)
        val domainHourly = forecastList.map { item ->
            ForecastHour(
                time = formatEpochToIsoString(item.dt),
                temp = item.main?.temp ?: 0.0,
                weatherCode = mapOpenWeatherCodeToWmo(item.weather?.firstOrNull()?.id),
                rainProbability = ((item.pop ?: 0.0) * 100).toInt()
            )
        }

        // Map Daily Forecast (group the 3-hourly items by date)
        val dailyGroups = forecastList.groupBy { formatEpochToDateString(it.dt) }
        val domainDaily = dailyGroups.map { (date, items) ->
            val tempMin = items.mapNotNull { it.main?.tempMin }.minOrNull() ?: 0.0
            val tempMax = items.mapNotNull { it.main?.tempMax }.maxOrNull() ?: 0.0
            val primaryItem = items.firstOrNull()
            val code = mapOpenWeatherCodeToWmo(primaryItem?.weather?.firstOrNull()?.id)

            ForecastDay(
                date = date,
                tempMin = tempMin,
                tempMax = tempMax,
                weatherCode = code,
                sunrise = currentSunrise,
                sunset = currentSunset,
                uvIndex = 0.0
            )
        }

        val currentRainProb = domainHourly.firstOrNull()?.rainProbability ?: 0

        // Generate Agromet Advisories
        val alerts = generateOpenWeatherAlerts(
            currentTemp = currentTemp,
            rainProbability = currentRainProb,
            windSpeed = windSpeedKmh,
            humidity = humidity,
            uvIndex = 0.0,
            baseAlerts = emptyList()
        )

        WeatherInfo(
            latitude = latitude,
            longitude = longitude,
            currentTemp = currentTemp,
            feelsLikeTemp = feelsLike,
            weatherCode = weatherCode,
            humidity = humidity,
            windSpeed = windSpeedKmh,
            precipitation = 0.0,
            rainProbability = currentRainProb,
            sunrise = currentSunrise,
            sunset = currentSunset,
            uvIndex = 0.0,
            time = currentIsoTime,
            isCached = false,
            lastCachedTime = System.currentTimeMillis(),
            hourlyForecast = domainHourly,
            dailyForecast = domainDaily,
            aqi = null,
            pm25 = null,
            pm10 = null,
            providerName = "OpenWeather",
            alerts = alerts
        )
    }

    private fun mapOpenWeatherCodeToWmo(code: Int?): Int {
        if (code == null) return 0
        return when (code) {
            in 200..232 -> 95 // Thunderstorm
            in 300..321 -> 51 // Light Drizzle
            500, 501 -> 61 // Slight Rain
            502, 503, 504 -> 65 // Heavy Rain
            511 -> 66 // Freezing Rain
            in 520..531 -> 80 // Rain Showers
            in 600..622 -> 73 // Moderate Snowfall
            in 701..781 -> 45 // Fog
            800 -> 0 // Clear Sky
            801 -> 1 // Mainly Clear
            802 -> 2 // Partly Cloudy
            803, 804 -> 3 // Overcast
            else -> 0
        }
    }

    private fun formatEpochToIsoString(seconds: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
        return sdf.format(Date(seconds * 1000L))
    }

    private fun formatEpochToDateString(seconds: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(seconds * 1000L))
    }

    private fun generateOpenWeatherAlerts(
        currentTemp: Double,
        rainProbability: Int,
        windSpeed: Double,
        humidity: Double,
        uvIndex: Double,
        baseAlerts: List<WeatherAlert>
    ): List<WeatherAlert> {
        val alerts = baseAlerts.toMutableList()
        
        // 1. Rain Warning / Delay Irrigation
        if (rainProbability > 60) {
            alerts.add(
                WeatherAlert(
                    title = "Heavy Rainfall Expected",
                    description = "Rain expected this evening. Delay irrigation.",
                    severity = "WARNING",
                    timing = "Next 6 Hours"
                )
            )
        }
        
        // 2. High Humidity / Fungal Risk
        if (humidity > 85.0) {
            alerts.add(
                WeatherAlert(
                    title = "Fungal Disease Risk",
                    description = "High humidity ($humidity%) may increase fungal disease risk.",
                    severity = "WARNING",
                    timing = "Current"
                )
            )
        }

        // 3. High Wind Warning / Strong Wind Conditions
        if (windSpeed >= 20.0) {
            alerts.add(
                WeatherAlert(
                    title = "Strong Wind Advisory",
                    description = "Strong wind conditions detected ($windSpeed km/h).",
                    severity = "WARNING",
                    timing = "Next 12 Hours"
                )
            )
        }
        
        // 4. Extreme Heat Alert
        if (currentTemp >= 35.0) {
            alerts.add(
                WeatherAlert(
                    title = "Extreme Heat Warning",
                    description = "High heat stress risk detected. Increase irrigation frequency to reduce moisture depletion.",
                    severity = "WARNING",
                    timing = "Next 24 Hours"
                )
            )
        }
        
        if (alerts.isEmpty()) {
            alerts.add(
                WeatherAlert(
                    title = "OpenWeather Agromet Advisory",
                    description = "Favorable environmental conditions. Optimal temperature, wind speed, and moisture for farming activities.",
                    severity = "INFO",
                    timing = "Current"
                )
            )
        }
        
        return alerts
    }
}
