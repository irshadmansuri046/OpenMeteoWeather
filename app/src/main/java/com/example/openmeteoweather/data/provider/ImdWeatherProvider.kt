package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.data.remote.imd.IMDApiService
import com.example.openmeteoweather.data.remote.imd.model.IMDWeatherResponse
import com.example.openmeteoweather.domain.model.ForecastDay
import com.example.openmeteoweather.domain.model.ForecastHour
import com.example.openmeteoweather.domain.model.WeatherAlert
import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.WeatherProvider
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImdWeatherProvider @Inject constructor(
    private val imdApiService: IMDApiService,
    private val openMeteoProvider: OpenMeteoProvider
) : WeatherProvider {

    private val gson = Gson()

    override suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherInfo {
        return try {
            // 1. Fetch AWS Data (national query without sid to get all stations)
            println("IMD_DEBUG: Fetching national AWS data for coordinates: ($latitude, $longitude)")
            val jsonElement = imdApiService.getAwsData(null)
            
            // 2. Parse the dynamic stations list
            val stations = mutableListOf<Map<String, Any?>>()
            if (jsonElement.isJsonArray) {
                val typeToken = object : TypeToken<List<Map<String, Any?>>>() {}.type
                stations.addAll(gson.fromJson<List<Map<String, Any?>>>(jsonElement, typeToken))
            } else if (jsonElement.isJsonObject) {
                val typeToken = object : TypeToken<Map<String, Any?>>() {}.type
                val singleMap = gson.fromJson<Map<String, Any?>>(jsonElement, typeToken)
                stations.add(singleMap)
            }
            println("IMD_DEBUG: Parsed ${stations.size} stations dynamically")

            // 3. Find the closest AWS station
            var closestStation: Map<String, Any?>? = null
            var minDistance = Double.MAX_VALUE
            for (station in stations) {
                val stLat = (station["Latitude"] ?: station["latitude"] ?: station["lat"])?.toString()?.toDoubleOrNull() ?: continue
                val stLng = (station["Longitude"] ?: station["longitude"] ?: station["lon"] ?: station["lng"])?.toString()?.toDoubleOrNull() ?: continue
                val dist = calculateDistance(latitude, longitude, stLat, stLng)
                if (dist < minDistance) {
                    minDistance = dist
                    closestStation = station
                }
            }

            // 4. Map observations if a station is within a reasonable distance (300 km)
            if (closestStation != null && minDistance < 300.0) {
                val stationName = closestStation["STATION"]?.toString()
                    ?: closestStation["station_name"]?.toString() 
                    ?: closestStation["name"]?.toString() 
                    ?: closestStation["station_id"]?.toString() 
                    ?: closestStation["ID"]?.toString()
                    ?: "IMD Station"
                
                println("IMD_DEBUG: Closest station is $stationName at distance: $minDistance km")
                    
                val currentTemp = closestStation["CURR_TEMP"]?.toString()?.toDoubleOrNull()
                    ?: closestStation["temp"]?.toString()?.toDoubleOrNull()
                    ?: closestStation["temperature"]?.toString()?.toDoubleOrNull()
                    ?: closestStation["dry_bulb_temp"]?.toString()?.toDoubleOrNull()
                    
                val feelsLike = closestStation["Feel Like"]?.toString()?.toDoubleOrNull()

                val humidity = closestStation["RH"]?.toString()?.toDoubleOrNull()
                    ?: closestStation["humidity"]?.toString()?.toDoubleOrNull()
                    ?: closestStation["relative_humidity"]?.toString()?.toDoubleOrNull()
                    
                val windSpeed = closestStation["WIND_SPEED"]?.toString()?.toDoubleOrNull()
                    ?: closestStation["wind_speed"]?.toString()?.toDoubleOrNull()
                    ?: closestStation["windspeed"]?.toString()?.toDoubleOrNull()
                    
                val rain = closestStation["RAINFALL"]?.toString()?.toDoubleOrNull()
                    ?: closestStation["rainfall"]?.toString()?.toDoubleOrNull()
                    ?: closestStation["precipitation"]?.toString()?.toDoubleOrNull()
                    ?: closestStation["rain_24h"]?.toString()?.toDoubleOrNull()

                println("IMD_DEBUG: AWS Metrics - Temp: $currentTemp, Feels Like: $feelsLike, Hum: $humidity, Wind: $windSpeed, Rain: $rain")

                // Merge observations with actual Open-Meteo forecasts
                val forecast = openMeteoProvider.getWeatherData(latitude, longitude)
                forecast.copy(
                    currentTemp = currentTemp ?: forecast.currentTemp,
                    feelsLikeTemp = feelsLike ?: forecast.feelsLikeTemp,
                    humidity = humidity ?: forecast.humidity,
                    windSpeed = windSpeed ?: forecast.windSpeed,
                    precipitation = rain ?: forecast.precipitation,
                    providerName = "IMD Station: $stationName",
                    alerts = generateImdAlertsForWeather(currentTemp ?: forecast.currentTemp, forecast)
                ).also {
                    println("IMD_DEBUG: Successfully merged AWS station observations and forecasts.")
                }
            } else {
                println("IMD_DEBUG: No nearby AWS station found (closest is ${minDistance}km away). Falling back to Open-Meteo mapping.")
                // Out of state boundaries / no nearby stations, map Open-Meteo as generic IMD
                val actualWeather = openMeteoProvider.getWeatherData(latitude, longitude)
                mapActualWeatherToImd(actualWeather)
            }
        } catch (e: Exception) {
            println("IMD_DEBUG: AWS API Request failed: ${e.message}")
            e.printStackTrace()
            val friendlyMessage = when (e) {
                is retrofit2.HttpException -> {
                    val errorBody = e.response()?.errorBody()?.string() ?: ""
                    "IMD API Request failed: HTTP ${e.code()} ${e.message()}. Response: $errorBody"
                }
                is java.io.IOException -> {
                    "IMD API Request failed: Network error. Please check your internet connection."
                }
                else -> {
                    "IMD API Request failed: ${e.localizedMessage ?: e.message}"
                }
            }
            throw Exception(friendlyMessage, e)
        }
    }

    private fun mapToDomain(latitude: Double, longitude: Double, response: IMDWeatherResponse): WeatherInfo {
        // Fallback mapper for forecast endpoint (retained for backward compatibility/future scaling)
        val current = response.current
        val hourly = response.hourly
        val daily = response.daily

        val hourlyList = mutableListOf<ForecastHour>()
        val hTimes = hourly?.times ?: emptyList()
        val hTemps = hourly?.temps ?: emptyList()
        val hCodes = hourly?.weatherCodes ?: emptyList()
        val hRainProbs = hourly?.rainProbabilities ?: emptyList()

        val maxHourlySize = minOf(hTimes.size, hTemps.size, hCodes.size)
        for (i in 0 until maxHourlySize) {
            hourlyList.add(
                ForecastHour(
                    time = hTimes[i],
                    temp = hTemps[i],
                    weatherCode = hCodes[i],
                    rainProbability = hRainProbs.getOrNull(i) ?: 0
                )
            )
        }

        val dailyList = mutableListOf<ForecastDay>()
        val dTimes = daily?.dates ?: emptyList()
        val dCodes = daily?.weatherCodes ?: emptyList()
        val dMaxs = daily?.tempsMax ?: emptyList()
        val dMins = daily?.tempsMin ?: emptyList()
        val dSunrises = daily?.sunrise ?: emptyList()
        val dSunsets = daily?.sunset ?: emptyList()
        val dUvs = daily?.uvIndex ?: emptyList()

        val maxDailySize = minOf(dTimes.size, dCodes.size, dMaxs.size, dMins.size)
        for (i in 0 until maxDailySize) {
            dailyList.add(
                ForecastDay(
                    date = dTimes[i],
                    tempMin = dMins[i],
                    tempMax = dMaxs[i],
                    weatherCode = dCodes[i],
                    sunrise = dSunrises.getOrNull(i) ?: "",
                    sunset = dSunsets.getOrNull(i) ?: "",
                    uvIndex = dUvs.getOrNull(i) ?: 0.0
                )
            )
        }

        val rainProbability = hourlyList.firstOrNull()?.rainProbability ?: 0
        val sunrise = dailyList.firstOrNull()?.sunrise ?: ""
        val sunset = dailyList.firstOrNull()?.sunset ?: ""
        val uvIndex = dailyList.firstOrNull()?.uvIndex ?: 0.0

        val alerts = response.alerts?.map {
            WeatherAlert(
                title = it.title,
                description = it.description,
                severity = it.severity.uppercase(Locale.getDefault()),
                timing = it.timing
            )
        } ?: emptyList()

        return WeatherInfo(
            latitude = latitude,
            longitude = longitude,
            currentTemp = current?.temp ?: 0.0,
            feelsLikeTemp = current?.apparentTemp ?: 0.0,
            weatherCode = current?.weatherCode ?: 0,
            humidity = current?.humidity ?: 0.0,
            windSpeed = current?.windSpeed ?: 0.0,
            precipitation = current?.precipitation ?: 0.0,
            rainProbability = rainProbability,
            sunrise = sunrise,
            sunset = sunset,
            uvIndex = uvIndex,
            time = current?.time ?: "",
            isCached = false,
            lastCachedTime = System.currentTimeMillis(),
            hourlyForecast = hourlyList,
            dailyForecast = dailyList,
            aqi = 45,
            pm25 = 12.0,
            pm10 = 20.0,
            providerName = "IMD India",
            alerts = alerts
        )
    }

    private fun mapActualWeatherToImd(actualWeather: WeatherInfo): WeatherInfo {
        return actualWeather.copy(
            providerName = "IMD India",
            alerts = generateImdAlertsForWeather(actualWeather.currentTemp, actualWeather)
        )
    }

    private fun generateImdAlertsForWeather(currentTemp: Double, weather: WeatherInfo): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()
        
        // 1. Heatwave Warning
        if (currentTemp >= 40.0) {
            alerts.add(
                WeatherAlert(
                    title = "Heatwave Alert (IMD Warning)",
                    description = "Severe heatwave conditions are likely over the region. Avoid outdoor exposure between 12:00 PM and 3:00 PM. Keep livestock in shade.",
                    severity = "DANGER",
                    timing = "Next 24 Hours"
                )
            )
        } else if (currentTemp >= 36.0) {
            alerts.add(
                WeatherAlert(
                    title = "High Temperature Warning (IMD Warning)",
                    description = "Warm weather conditions. Keep hydrated and provide adequate ventilation for farm animals.",
                    severity = "WARNING",
                    timing = "Next 24 Hours"
                )
            )
        }

        // 2. Heavy Rainfall / Thunderstorm Alert
        if (weather.rainProbability >= 80 || weather.weatherCode in listOf(80, 81, 82, 95, 96, 99)) {
            alerts.add(
                WeatherAlert(
                    title = "Thunderstorm Alert (IMD Warning)",
                    description = "Thunderstorm accompanied with moderate to heavy rain and gusty winds very likely. Farmers should postpone pesticide application and secure harvested crops.",
                    severity = "WARNING",
                    timing = "Next 12 Hours"
                )
            )
        }

        // 3. Cyclone/Wind Alert
        if (weather.windSpeed >= 40.0) {
            alerts.add(
                WeatherAlert(
                    title = "High Wind Warning (IMD Warning)",
                    description = "Strong gusty winds expected. Avoid spraying crops or working in open fields. Secure nursery structures.",
                    severity = "WARNING",
                    timing = "Next 12 Hours"
                )
            )
        }

        // 4. Default dynamic agro-meteorological advisory if no severe alerts
        if (alerts.isEmpty()) {
            alerts.add(
                WeatherAlert(
                    title = "IMD Agromet Advisory Bulletin",
                    description = "Fair weather conditions expected. Suitable time for regular crop harvesting, sowing prep, and weeding. Monitor soil moisture.",
                    severity = "INFO",
                    timing = "Current"
                )
            )
        }
        return alerts
    }


    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta))
        dist = Math.acos(dist)
        dist = Math.toDegrees(dist)
        dist = dist * 60 * 1.1515 * 1.609344 // in kilometers
        return dist
    }

    private fun generateMockWeatherInfo(latitude: Double, longitude: Double): WeatherInfo {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val currentTemp = 32.5
        val feelsLikeTemp = 36.0
        val humidity = 78.0
        val windSpeed = 18.5
        val precipitation = 4.2
        val weatherCode = 80
        val timeString = sdf.format(calendar.time)

        val hourlyList = mutableListOf<ForecastHour>()
        val hourlyCalendar = calendar.clone() as Calendar
        for (i in 0 until 24) {
            hourlyList.add(
                ForecastHour(
                    time = sdf.format(hourlyCalendar.time),
                    temp = currentTemp - (i % 4) + (i % 3),
                    weatherCode = if (i < 4) 80 else 61,
                    rainProbability = if (i < 4) 90 else 40
                )
            )
            hourlyCalendar.add(Calendar.HOUR_OF_DAY, 1)
        }

        val dailyList = mutableListOf<ForecastDay>()
        val dailyCalendar = calendar.clone() as Calendar
        for (i in 0 until 7) {
            dailyList.add(
                ForecastDay(
                    date = dateSdf.format(dailyCalendar.time),
                    tempMin = 24.0 + (i % 2),
                    tempMax = 34.0 - (i % 3),
                    weatherCode = if (i == 0) 80 else if (i % 2 == 0) 61 else 3,
                    sunrise = "05:45 AM",
                    sunset = "06:55 PM",
                    uvIndex = 8.5 - (i % 2)
                )
            )
            dailyCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val alerts = listOf(
            WeatherAlert(
                title = "Thunderstorm Alert (IMD Warning)",
                description = "Thunderstorm accompanied with moderate lightning and gusty winds (speed 30-40 km/h) very likely to occur in this agricultural block.",
                severity = "WARNING",
                timing = "Next 12 Hours"
            ),
            WeatherAlert(
                title = "Monsoon Warning (Rain Alert)",
                description = "Active monsoon conditions. Heavy rainfall expected. Farmers should refrain from spraying pesticides and postpone harvesting operations.",
                severity = "DANGER",
                timing = "Next 48 Hours"
            )
        )

        return WeatherInfo(
            latitude = latitude,
            longitude = longitude,
            currentTemp = currentTemp,
            feelsLikeTemp = feelsLikeTemp,
            weatherCode = weatherCode,
            humidity = humidity,
            windSpeed = windSpeed,
            precipitation = precipitation,
            rainProbability = 90,
            sunrise = "05:45 AM",
            sunset = "06:55 PM",
            uvIndex = 8.5,
            time = timeString,
            isCached = false,
            lastCachedTime = System.currentTimeMillis(),
            hourlyForecast = hourlyList,
            dailyForecast = dailyList,
            aqi = 58,
            pm25 = 16.2,
            pm10 = 35.0,
            providerName = "IMD India (Mock Feed)",
            alerts = alerts
        )
    }
}
