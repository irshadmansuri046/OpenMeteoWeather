package com.example.openmeteoweather.data.repository

import com.example.openmeteoweather.data.local.ImdWeatherEntity
import com.example.openmeteoweather.data.local.OpenWeatherWeatherEntity
import com.example.openmeteoweather.data.local.OpenWeatherAlertEntity
import com.example.openmeteoweather.data.local.CombinedWeatherEntity
import com.example.openmeteoweather.data.local.ProviderAgreementEntity
import com.example.openmeteoweather.data.local.SearchHistoryEntity
import com.example.openmeteoweather.data.local.WeatherDao
import com.example.openmeteoweather.data.local.WeatherEntity
import com.example.openmeteoweather.data.local.SwanWeatherEntity
import com.example.openmeteoweather.data.provider.WeatherProviderManager
import com.example.openmeteoweather.data.remote.GeocodingApi
import com.example.openmeteoweather.domain.model.ForecastDay
import com.example.openmeteoweather.domain.model.ForecastHour
import com.example.openmeteoweather.domain.model.LocationInfo
import com.example.openmeteoweather.domain.model.WeatherAlert
import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.ProviderType
import com.example.openmeteoweather.domain.repository.WeatherRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val providerManager: WeatherProviderManager,
    private val geocodingApi: GeocodingApi,
    private val dao: WeatherDao
) : WeatherRepository {

    private val gson = Gson()
    private val cacheExpirationMillis = 3600000 // 1 hour

    override suspend fun getWeatherData(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean
    ): WeatherInfo {
        val providerType = providerManager.getActiveProviderType()
        val cacheKey = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)

        if (!forceRefresh) {
            // Check cache based on active provider
            when (providerType) {
                ProviderType.OPEN_METEO -> {
                    val cachedEntity = dao.getWeatherCache(latitude, longitude)
                    val isCacheValid = cachedEntity != null && 
                            (System.currentTimeMillis() - cachedEntity.lastUpdated < cacheExpirationMillis)
                    if (isCacheValid && cachedEntity != null) {
                        return mapEntityToDomain(cachedEntity)
                    }
                }
                ProviderType.IMD -> {
                    val cachedEntity = dao.getImdWeatherCache(latitude, longitude)
                    val isCacheValid = cachedEntity != null && 
                            (System.currentTimeMillis() - cachedEntity.lastUpdated < cacheExpirationMillis)
                    if (isCacheValid && cachedEntity != null) {
                        return gson.fromJson(cachedEntity.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                    }
                }
                ProviderType.AMBEE -> {
                    val cachedEntity = dao.getAmbeeWeatherCache(latitude, longitude)
                    val isCacheValid = cachedEntity != null && 
                            (System.currentTimeMillis() - cachedEntity.lastUpdated < cacheExpirationMillis)
                    if (isCacheValid && cachedEntity != null) {
                        return gson.fromJson(cachedEntity.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                    }
                }
                ProviderType.TOMORROW_IO -> {
                    val cachedEntity = dao.getTomorrowWeatherCache(latitude, longitude)
                    val isCacheValid = cachedEntity != null && 
                            (System.currentTimeMillis() - cachedEntity.lastUpdated < cacheExpirationMillis)
                    if (isCacheValid && cachedEntity != null) {
                        return gson.fromJson(cachedEntity.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                    }
                }
                ProviderType.OPEN_WEATHER -> {
                    val cachedEntity = dao.getOpenWeatherCache(latitude, longitude)
                    val isCacheValid = cachedEntity != null && 
                            (System.currentTimeMillis() - cachedEntity.lastUpdated < cacheExpirationMillis)
                    if (isCacheValid && cachedEntity != null) {
                        return gson.fromJson(cachedEntity.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                    }
                }
                ProviderType.SWAN_WEATHER -> {
                    val cachedEntity = dao.getSwanWeatherCache(latitude, longitude)
                    val isCacheValid = cachedEntity != null && 
                            (System.currentTimeMillis() - cachedEntity.lastUpdated < cacheExpirationMillis)
                    if (isCacheValid && cachedEntity != null) {
                        return gson.fromJson(cachedEntity.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                    }
                }
                ProviderType.COMBINED_WEATHER -> {
                    val cachedEntity = dao.getCombinedWeatherCache(latitude, longitude)
                    val isCacheValid = cachedEntity != null && 
                            (System.currentTimeMillis() - cachedEntity.lastUpdated < cacheExpirationMillis)
                    if (isCacheValid && cachedEntity != null) {
                        return gson.fromJson(cachedEntity.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                    }
                }
            }
        }

        // Fetch from providers in order of cascading priority fallback
        val chain = when (providerType) {
            ProviderType.IMD -> listOf(ProviderType.IMD, ProviderType.TOMORROW_IO, ProviderType.SWAN_WEATHER, ProviderType.AMBEE, ProviderType.OPEN_WEATHER, ProviderType.OPEN_METEO)
            ProviderType.TOMORROW_IO -> listOf(ProviderType.TOMORROW_IO, ProviderType.SWAN_WEATHER, ProviderType.AMBEE, ProviderType.OPEN_WEATHER, ProviderType.OPEN_METEO)
            ProviderType.SWAN_WEATHER -> listOf(ProviderType.SWAN_WEATHER, ProviderType.TOMORROW_IO, ProviderType.AMBEE, ProviderType.OPEN_WEATHER, ProviderType.OPEN_METEO)
            ProviderType.AMBEE -> listOf(ProviderType.AMBEE, ProviderType.OPEN_WEATHER, ProviderType.OPEN_METEO)
            ProviderType.OPEN_WEATHER -> listOf(ProviderType.OPEN_WEATHER, ProviderType.OPEN_METEO)
            ProviderType.OPEN_METEO -> listOf(ProviderType.OPEN_METEO)
            ProviderType.COMBINED_WEATHER -> listOf(ProviderType.COMBINED_WEATHER, ProviderType.OPEN_METEO)
        }

        var lastException: Exception? = null
        val startTime = System.currentTimeMillis()

        for (type in chain) {
            val provider = providerManager.getProvider(type)
            try {
                Timber.d("Fetching weather data from provider: %s", type.name)
                val fetchStartTime = System.currentTimeMillis()
                val weatherInfo = provider.getWeatherData(latitude, longitude)
                val fetchDuration = System.currentTimeMillis() - fetchStartTime
                Timber.d("Fetched weather data from provider %s in %d ms", type.name, fetchDuration)

                // If this is a fallback provider, we prepend a non-blocking alert to inform the user
                val finalizedWeatherInfo = if (type != providerType) {
                    val fallbackAlert = when (providerType) {
                        ProviderType.IMD -> WeatherAlert(
                            title = "IMD Weather Temporarily Unavailable",
                            description = "IMD Weather temporarily unavailable. Showing backup weather source.",
                            severity = "WARNING",
                            timing = "Current"
                        )
                        ProviderType.OPEN_WEATHER -> WeatherAlert(
                            title = "OpenWeather Temporarily Unavailable",
                            description = "OpenWeather temporarily unavailable. Showing backup weather source.",
                            severity = "WARNING",
                            timing = "Current"
                        )
                        ProviderType.SWAN_WEATHER -> WeatherAlert(
                            title = "SwanWeather Temporarily Unavailable",
                            description = "SwanWeather temporarily unavailable. Showing backup weather source.",
                            severity = "WARNING",
                            timing = "Current"
                        )
                        ProviderType.TOMORROW_IO -> WeatherAlert(
                            title = "Tomorrow.io Temporarily Unavailable",
                            description = "Tomorrow.io temporarily unavailable. Showing backup weather source.",
                            severity = "WARNING",
                            timing = "Current"
                        )
                        ProviderType.AMBEE -> WeatherAlert(
                            title = "Ambee Temporarily Unavailable",
                            description = "Ambee Forecast temporarily unavailable. Showing backup weather source.",
                            severity = "WARNING",
                            timing = "Current"
                        )
                        else -> null
                    }
                    if (fallbackAlert != null) {
                        val updatedAlerts = mutableListOf(fallbackAlert)
                        updatedAlerts.addAll(weatherInfo.alerts)
                        weatherInfo.copy(alerts = updatedAlerts)
                    } else {
                        weatherInfo
                    }
                } else {
                    weatherInfo
                }

                // Cache the result under the provider that succeeded
                cacheWeatherData(cacheKey, latitude, longitude, type, finalizedWeatherInfo)
                return finalizedWeatherInfo
            } catch (e: Exception) {
                Timber.e(e, "Error fetching from provider: %s", type.name)
                lastException = e
            }
        }

        val totalDuration = System.currentTimeMillis() - startTime
        Timber.e("All weather providers in failover chain failed. Total duration: %d ms", totalDuration)
        throw lastException ?: Exception("Failed to fetch weather data from all available providers")
    }

    override suspend fun searchLocation(query: String): List<LocationInfo> {
        val response = geocodingApi.searchLocation(query)
        return response.results?.map {
            LocationInfo(
                id = it.id,
                name = it.name,
                latitude = it.latitude,
                longitude = it.longitude,
                country = it.country,
                admin1 = it.admin1,
                timezone = it.timezone
            )
        } ?: emptyList()
    }

    override fun getRecentSearches(): Flow<List<LocationInfo>> {
        return dao.getSearchHistory().map { list ->
            list.map {
                LocationInfo(
                    id = it.id,
                    name = it.name,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    country = it.country,
                    admin1 = it.admin1,
                    timezone = it.timezone
                )
            }
        }
    }

    override suspend fun saveRecentSearch(location: LocationInfo) {
        dao.insertSearchHistory(
            SearchHistoryEntity(
                id = location.id,
                name = location.name,
                latitude = location.latitude,
                longitude = location.longitude,
                country = location.country,
                admin1 = location.admin1,
                timezone = location.timezone,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteRecentSearch(location: LocationInfo) {
        dao.deleteSearchHistory(
            SearchHistoryEntity(
                id = location.id,
                name = location.name,
                latitude = location.latitude,
                longitude = location.longitude,
                country = location.country,
                admin1 = location.admin1,
                timezone = location.timezone,
                timestamp = 0L
            )
        )
    }

    override suspend fun clearRecentSearches() {
        dao.clearSearchHistory()
    }

    private suspend fun cacheWeatherData(
        id: String,
        latitude: Double,
        longitude: Double,
        providerType: ProviderType,
        weatherInfo: WeatherInfo
    ) {
        when (providerType) {
            ProviderType.OPEN_METEO -> {
                val entity = WeatherEntity(
                    id = id,
                    latitude = weatherInfo.latitude,
                    longitude = weatherInfo.longitude,
                    time = weatherInfo.time,
                    temperature = weatherInfo.currentTemp,
                    humidity = weatherInfo.humidity,
                    apparentTemperature = weatherInfo.feelsLikeTemp,
                    weatherCode = weatherInfo.weatherCode,
                    windSpeed = weatherInfo.windSpeed,
                    precipitation = weatherInfo.precipitation,
                    hourlyTimeJson = gson.toJson(weatherInfo.hourlyForecast.map { it.time }),
                    hourlyTemperaturesJson = gson.toJson(weatherInfo.hourlyForecast.map { it.temp }),
                    hourlyWeatherCodesJson = gson.toJson(weatherInfo.hourlyForecast.map { it.weatherCode }),
                    hourlyPrecipitationProbabilitiesJson = gson.toJson(weatherInfo.hourlyForecast.map { it.rainProbability }),
                    dailyTimeJson = gson.toJson(weatherInfo.dailyForecast.map { it.date }),
                    dailyWeatherCodesJson = gson.toJson(weatherInfo.dailyForecast.map { it.weatherCode }),
                    dailyTemperaturesMaxJson = gson.toJson(weatherInfo.dailyForecast.map { it.tempMax }),
                    dailyTemperaturesMinJson = gson.toJson(weatherInfo.dailyForecast.map { it.tempMin }),
                    dailySunriseJson = gson.toJson(weatherInfo.dailyForecast.map { it.sunrise }),
                    dailySunsetJson = gson.toJson(weatherInfo.dailyForecast.map { it.sunset }),
                    dailyUvIndexMaxJson = gson.toJson(weatherInfo.dailyForecast.map { it.uvIndex }),
                    usAqi = weatherInfo.aqi,
                    pm25 = weatherInfo.pm25,
                    pm10 = weatherInfo.pm10,
                    lastUpdated = System.currentTimeMillis()
                )
                dao.insertWeather(entity)
            }
            ProviderType.IMD -> {
                val entity = ImdWeatherEntity(
                    id = id,
                    latitude = latitude,
                    longitude = longitude,
                    weatherInfoJson = gson.toJson(weatherInfo),
                    lastUpdated = System.currentTimeMillis()
                )
                dao.insertImdWeather(entity)
            }
            ProviderType.AMBEE -> {
                val entity = com.example.openmeteoweather.data.local.AmbeeWeatherEntity(
                    id = id,
                    latitude = latitude,
                    longitude = longitude,
                    weatherInfoJson = gson.toJson(weatherInfo),
                    lastUpdated = System.currentTimeMillis()
                )
                dao.insertAmbeeWeather(entity)

                if (weatherInfo.aqi != null) {
                    val aqiEntity = com.example.openmeteoweather.data.local.AmbeeAirQualityEntity(
                        id = id,
                        latitude = latitude,
                        longitude = longitude,
                        aqi = weatherInfo.aqi,
                        pm25 = weatherInfo.pm25 ?: 0.0,
                        pm10 = weatherInfo.pm10 ?: 0.0,
                        co = weatherInfo.co ?: 0.0,
                        no2 = weatherInfo.no2 ?: 0.0,
                        ozone = weatherInfo.ozone ?: 0.0,
                        so2 = weatherInfo.so2 ?: 0.0,
                        category = weatherInfo.alerts.firstOrNull { it.title.contains("Air Quality") }?.description ?: "Moderate",
                        dominantPollutant = "PM2.5",
                        lastUpdated = System.currentTimeMillis()
                    )
                    dao.insertAmbeeAirQuality(aqiEntity)
                }
            }
            ProviderType.TOMORROW_IO -> {
                val entity = com.example.openmeteoweather.data.local.TomorrowWeatherEntity(
                    id = id,
                    latitude = latitude,
                    longitude = longitude,
                    weatherInfoJson = gson.toJson(weatherInfo),
                    lastUpdated = System.currentTimeMillis()
                )
                dao.insertTomorrowWeather(entity)
            }
            ProviderType.SWAN_WEATHER -> {
                val entity = SwanWeatherEntity(
                    id = id,
                    latitude = latitude,
                    longitude = longitude,
                    weatherInfoJson = gson.toJson(weatherInfo),
                    lastUpdated = System.currentTimeMillis()
                )
                dao.insertSwanWeather(entity)
            }
            ProviderType.OPEN_WEATHER -> {
                val entity = com.example.openmeteoweather.data.local.OpenWeatherWeatherEntity(
                    id = id,
                    latitude = latitude,
                    longitude = longitude,
                    weatherInfoJson = gson.toJson(weatherInfo),
                    lastUpdated = System.currentTimeMillis()
                )
                dao.insertOpenWeather(entity)

                if (weatherInfo.alerts.isNotEmpty()) {
                    val alertsEntity = com.example.openmeteoweather.data.local.OpenWeatherAlertEntity(
                        id = id,
                        latitude = latitude,
                        longitude = longitude,
                        alertsJson = gson.toJson(weatherInfo.alerts),
                        lastUpdated = System.currentTimeMillis()
                    )
                    dao.insertOpenWeatherAlerts(alertsEntity)
                }
            }
            ProviderType.COMBINED_WEATHER -> {
                val entity = CombinedWeatherEntity(
                    id = id,
                    latitude = latitude,
                    longitude = longitude,
                    weatherInfoJson = gson.toJson(weatherInfo),
                    lastUpdated = System.currentTimeMillis()
                )
                dao.insertCombinedWeather(entity)

                val agreementEntity = ProviderAgreementEntity(
                    id = id,
                    latitude = latitude,
                    longitude = longitude,
                    agreementPercentage = weatherInfo.agreementPercentage ?: 100.0,
                    confidenceScore = weatherInfo.confidenceScore ?: 100.0,
                    confidenceLevel = weatherInfo.confidenceLevel ?: "Very High",
                    rawComparisonJson = weatherInfo.providerBreakdownsJson ?: "[]",
                    lastUpdated = System.currentTimeMillis()
                )
                dao.insertProviderAgreement(agreementEntity)
            }
        }
    }

    private suspend fun retrieveStaleCache(
        latitude: Double,
        longitude: Double,
        providerType: ProviderType
    ): WeatherInfo? {
        return when (providerType) {
            ProviderType.OPEN_METEO -> {
                dao.getWeatherCache(latitude, longitude)?.let {
                    mapEntityToDomain(it).copy(isCached = true)
                }
            }
            ProviderType.IMD -> {
                dao.getImdWeatherCache(latitude, longitude)?.let {
                    gson.fromJson(it.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                }
            }
            ProviderType.AMBEE -> {
                dao.getAmbeeWeatherCache(latitude, longitude)?.let {
                    gson.fromJson(it.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                }
            }
            ProviderType.TOMORROW_IO -> {
                dao.getTomorrowWeatherCache(latitude, longitude)?.let {
                    gson.fromJson(it.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                }
            }
            ProviderType.OPEN_WEATHER -> {
                dao.getOpenWeatherCache(latitude, longitude)?.let {
                    gson.fromJson(it.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                }
            }
            ProviderType.SWAN_WEATHER -> {
                dao.getSwanWeatherCache(latitude, longitude)?.let {
                    gson.fromJson(it.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                }
            }
            ProviderType.COMBINED_WEATHER -> {
                dao.getCombinedWeatherCache(latitude, longitude)?.let {
                    gson.fromJson(it.weatherInfoJson, WeatherInfo::class.java).copy(isCached = true)
                }
            }
        }
    }

    private fun mapEntityToDomain(entity: WeatherEntity): WeatherInfo {
        val stringListType = object : TypeToken<List<String>>() {}.type
        val doubleListType = object : TypeToken<List<Double>>() {}.type
        val intListType = object : TypeToken<List<Int>>() {}.type

        val hTimes: List<String> = gson.fromJson(entity.hourlyTimeJson, stringListType)
        val hTemps: List<Double> = gson.fromJson(entity.hourlyTemperaturesJson, doubleListType)
        val hCodes: List<Int> = gson.fromJson(entity.hourlyWeatherCodesJson, intListType)
        val hRainProbs: List<Int> = gson.fromJson(entity.hourlyPrecipitationProbabilitiesJson, intListType)

        val hourlyList = mutableListOf<ForecastHour>()
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

        val dTimes: List<String> = gson.fromJson(entity.dailyTimeJson, stringListType)
        val dCodes: List<Int> = gson.fromJson(entity.dailyWeatherCodesJson, intListType)
        val dMaxs: List<Double> = gson.fromJson(entity.dailyTemperaturesMaxJson, doubleListType)
        val dMins: List<Double> = gson.fromJson(entity.dailyTemperaturesMinJson, doubleListType)
        val dSunrises: List<String> = gson.fromJson(entity.dailySunriseJson, stringListType)
        val dSunsets: List<String> = gson.fromJson(entity.dailySunsetJson, stringListType)
        val dUvs: List<Double> = gson.fromJson(entity.dailyUvIndexMaxJson, doubleListType)

        val dailyList = mutableListOf<ForecastDay>()
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

        val alerts = mutableListOf<WeatherAlert>()
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

        return WeatherInfo(
            latitude = entity.latitude,
            longitude = entity.longitude,
            currentTemp = entity.temperature,
            feelsLikeTemp = entity.apparentTemperature,
            weatherCode = entity.weatherCode,
            humidity = entity.humidity,
            windSpeed = entity.windSpeed,
            precipitation = entity.precipitation,
            rainProbability = rainProbability,
            sunrise = sunrise,
            sunset = sunset,
            uvIndex = uvIndex,
            time = entity.time,
            isCached = true,
            lastCachedTime = entity.lastUpdated,
            hourlyForecast = hourlyList,
            dailyForecast = dailyList,
            aqi = entity.usAqi,
            pm25 = entity.pm25,
            pm10 = entity.pm10,
            providerName = "Open-Meteo",
            alerts = alerts
        )
    }
}
