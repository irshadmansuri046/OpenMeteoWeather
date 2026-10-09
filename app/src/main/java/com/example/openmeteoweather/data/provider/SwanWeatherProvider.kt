package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.data.local.WeatherPreferences
import com.example.openmeteoweather.data.remote.swan.SwanWeatherApiService
import com.example.openmeteoweather.data.remote.swan.SwanCropStageQuery
import com.example.openmeteoweather.data.remote.swan.SwanCustomWeatherRequest
import com.example.openmeteoweather.domain.model.ForecastDay
import com.example.openmeteoweather.domain.model.ForecastHour
import com.example.openmeteoweather.domain.model.WeatherAlert
import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.WeatherProvider
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SwanWeatherProvider @Inject constructor(
    private val swanWeatherApiService: SwanWeatherApiService,
    private val preferences: WeatherPreferences
) : WeatherProvider {

    override suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherInfo {
        return try {
            val selectedCrop = preferences.selectedCrop.first()
            val selectedCropStage = preferences.selectedCropStage.first()

            val cropIdInt = selectedCrop?.cropId?.toIntOrNull()
            val stageIdInt = selectedCropStage?.stageId?.toIntOrNull()

            val cropStages = if (cropIdInt != null && stageIdInt != null) {
                listOf(SwanCropStageQuery(cropId = cropIdInt, stageId = stageIdInt))
            } else {
                emptyList()
            }

            var finalLat = latitude
            var finalLon = longitude
            var isDemoLocation = false

            var request = SwanCustomWeatherRequest(
                latitude = finalLat,
                longitude = finalLon,
                cropStages = cropStages
            )

            val response = try {
                swanWeatherApiService.getCustomWeather(request)
            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: ""
                if (e.code() == 500 && errorBody.contains("forecast_time")) {
                    isDemoLocation = true
                    finalLat = 10.0
                    finalLon = 76.0
                    request = request.copy(latitude = finalLat, longitude = finalLon)
                    try {
                        swanWeatherApiService.getCustomWeather(request)
                    } catch (innerE: retrofit2.HttpException) {
                        if (innerE.code() == 500 && request.cropStages.isNotEmpty()) {
                            swanWeatherApiService.getCustomWeather(request.copy(cropStages = emptyList()))
                        } else {
                            throw innerE
                        }
                    }
                } else if (e.code() == 500 && request.cropStages.isNotEmpty()) {
                    swanWeatherApiService.getCustomWeather(request.copy(cropStages = emptyList()))
                } else {
                    throw e
                }
            }
            
            // Map parameters to lists/maps
            val hourlyTimes = mutableListOf<String>()
            val tempMap = mutableMapOf<String, Double>()
            val precipMap = mutableMapOf<String, Double>()
            val humidityMap = mutableMapOf<String, Double>()
            val windSpeedMap = mutableMapOf<String, Double>()
            val windDirMap = mutableMapOf<String, Double>()
            val dewPointMap = mutableMapOf<String, Double>()
            val cloudCoverMap = mutableMapOf<String, Double>()

            val dailyParameters = response.data?.daily ?: emptyList()
            for (param in dailyParameters) {
                val dates = param.coordinates?.firstOrNull()?.dates ?: emptyList()
                for (dateVal in dates) {
                    val dateStr = dateVal.date ?: continue
                    val value = dateVal.value?.toDoubleOrNull() ?: 0.0
                    val cleanHour = if (dateStr.length >= 16) dateStr.substring(0, 16) else dateStr
                    when (param.parameter) {
                        "temperature" -> {
                            if (!hourlyTimes.contains(cleanHour)) hourlyTimes.add(cleanHour)
                            tempMap[cleanHour] = value
                        }
                        "precipitation" -> precipMap[cleanHour] = value
                        "relative_humidity" -> humidityMap[cleanHour] = value
                        "wind_speed" -> windSpeedMap[cleanHour] = value
                        "wind_direction" -> windDirMap[cleanHour] = value
                        "dew_point" -> dewPointMap[cleanHour] = value
                        "mean_effective_cloud_cover" -> cloudCoverMap[cleanHour] = value
                    }
                }
            }

            // Map hourly
            val domainHourly = hourlyTimes.map { timeVal ->
                val tempVal = tempMap[timeVal] ?: 0.0
                val precipVal = precipMap[timeVal] ?: 0.0
                val cloudVal = cloudCoverMap[timeVal] ?: 0.0
                val wmo = estimateWmoCode(precipVal, cloudVal)
                val rainProb = if (precipVal > 0.0) 100 else if (cloudVal > 50) 40 else 0
                ForecastHour(
                    time = timeVal,
                    temp = tempVal,
                    weatherCode = wmo,
                    rainProbability = rainProb
                )
            }

            // Collect weekly (daily) parameters
            val dailyTimes = mutableListOf<String>()
            val dailyTempMinMap = mutableMapOf<String, Double>()
            val dailyTempMaxMap = mutableMapOf<String, Double>()
            val dailyPrecipProbMap = mutableMapOf<String, Double>()

            val weeklyParameters = response.data?.weekly ?: emptyList()
            for (param in weeklyParameters) {
                val dates = param.coordinates?.firstOrNull()?.dates ?: emptyList()
                for (dateVal in dates) {
                    val dateStr = dateVal.date ?: continue
                    val value = dateVal.value?.toDoubleOrNull() ?: 0.0
                    val cleanDate = dateStr.substringBefore("T")
                    when (param.parameter) {
                        "min_temperature" -> {
                            if (!dailyTimes.contains(cleanDate)) dailyTimes.add(cleanDate)
                            dailyTempMinMap[cleanDate] = value
                        }
                        "max_temperature" -> {
                            if (!dailyTimes.contains(cleanDate)) dailyTimes.add(cleanDate)
                            dailyTempMaxMap[cleanDate] = value
                        }
                        "precipitation_probability" -> dailyPrecipProbMap[cleanDate] = value
                    }
                }
            }

            // Map daily
            val domainDaily = dailyTimes.map { dateVal ->
                val tempMinVal = dailyTempMinMap[dateVal] ?: 0.0
                val tempMaxVal = dailyTempMaxMap[dateVal] ?: 0.0
                val precipProb = dailyPrecipProbMap[dateVal]?.toInt() ?: 0
                val wmo = if (precipProb > 50) 63 else 0
                ForecastDay(
                    date = dateVal,
                    tempMin = tempMinVal,
                    tempMax = tempMaxVal,
                    weatherCode = wmo,
                    sunrise = "",
                    sunset = "",
                    uvIndex = 0.0
                )
            }

            // Extract current metrics from first hourly value
            val firstHourTime = hourlyTimes.firstOrNull() ?: ""
            val currentTemp = tempMap[firstHourTime] ?: 25.0
            val feelsLike = currentTemp
            val humidity = humidityMap[firstHourTime] ?: 60.0
            val windSpeed = windSpeedMap[firstHourTime] ?: 5.0
            val precipitation = precipMap[firstHourTime] ?: 0.0
            val cloudCover = cloudCoverMap[firstHourTime] ?: 0.0
            val weatherCode = estimateWmoCode(precipitation, cloudCover)
            val rainProb = if (precipitation > 0.0) 100 else if (cloudCover > 50) 40 else 0
            val uv = 0.0
            val timeString = firstHourTime

            val alerts = generateSwanAlerts(
                currentTemp = currentTemp,
                rainProbability = rainProb,
                windSpeed = windSpeed,
                uvIndex = uv
            ).toMutableList()

            if (isDemoLocation) {
                alerts.add(
                    0,
                    WeatherAlert(
                        title = "Using Demo Location",
                        description = "SwanWeather only covers India. Displaying weather and crop advisories for Kochi, Kerala, India (Lat: 10, Lon: 76).",
                        severity = "INFO",
                        timing = "Current"
                    )
                )
            }

            // Serialize advisories list to JSON string
            val customAdvisoriesJson = response.advisories?.let { Gson().toJson(it) }

            WeatherInfo(
                latitude = latitude,
                longitude = longitude,
                currentTemp = currentTemp,
                feelsLikeTemp = feelsLike,
                weatherCode = weatherCode,
                humidity = humidity,
                windSpeed = windSpeed,
                precipitation = precipitation,
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
                providerName = "SwanWeather",
                alerts = alerts,
                customAdvisoriesJson = customAdvisoriesJson
            )
        } catch (e: Exception) {
            println("SwanWeather_DEBUG: SwanWeather API Request failed: ${e.message}")
            e.printStackTrace()
            val friendlyMessage = when (e) {
                is retrofit2.HttpException -> {
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    "SwanWeather API Request failed: HTTP ${e.code()} ${e.message()}. Response: $errorBody"
                }
                is java.io.IOException -> {
                    "SwanWeather API Request failed: Network error. Please check your internet connection."
                }
                else -> {
                    "SwanWeather API Request failed: ${e.localizedMessage ?: e.message}"
                }
            }
            throw Exception(friendlyMessage, e)
        }
    }

    private fun estimateWmoCode(precipitation: Double, cloudCover: Double): Int {
        return if (precipitation > 0.1) {
            63 // Rain
        } else if (cloudCover > 80.0) {
            3 // Overcast
        } else if (cloudCover > 20.0) {
            2 // Partly cloudy
        } else {
            0 // Clear
        }
    }

    private fun generateSwanAlerts(
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

        // 3. Wind Warning (Assuming wind speed is in m/s, threshold is ~7 m/s (~25 km/h))
        if (windSpeed >= 7.0 || windSpeed >= 25.0) {
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
        if (rainProbability > 50 || windSpeed >= 25.0 || windSpeed >= 7.0) {
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
                    title = "SwanWeather Agromet Advisory",
                    description = "Favorable environmental conditions. Optimal temperature, wind speed, and moisture for farming activities.",
                    severity = "INFO",
                    timing = "Current"
                )
            )
        }

        return alerts
    }
}
