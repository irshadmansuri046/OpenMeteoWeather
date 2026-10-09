package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.data.remote.AirQualityApi
import com.example.openmeteoweather.data.remote.WeatherApi
import com.example.openmeteoweather.data.remote.dto.AirQualityResponseDto
import com.example.openmeteoweather.data.remote.dto.WeatherResponseDto
import com.example.openmeteoweather.domain.model.ForecastDay
import com.example.openmeteoweather.domain.model.ForecastHour
import com.example.openmeteoweather.domain.model.WeatherAlert
import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.WeatherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenMeteoProvider @Inject constructor(
    private val weatherApi: WeatherApi,
    private val airQualityApi: AirQualityApi
) : WeatherProvider {

    override suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherInfo = coroutineScope {
        val weatherDeferred = async { weatherApi.getForecast(latitude, longitude) }
        val aqiDeferred = async { airQualityApi.getAirQuality(latitude, longitude) }

        val weatherResponse = weatherDeferred.await()
        val aqiResponse = aqiDeferred.await()

        mapToDomain(latitude, longitude, weatherResponse, aqiResponse)
    }

    private fun mapToDomain(
        latitude: Double,
        longitude: Double,
        weather: WeatherResponseDto,
        aqi: AirQualityResponseDto
    ): WeatherInfo {
        val current = weather.current
        val hourly = weather.hourly
        val daily = weather.daily

        val hourlyList = mutableListOf<ForecastHour>()
        val hTimes = hourly?.time ?: emptyList()
        val hTemps = hourly?.temperatures ?: emptyList()
        val hCodes = hourly?.weatherCodes ?: emptyList()
        val hRainProbs = hourly?.precipitationProbabilities ?: emptyList()

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
        val dTimes = daily?.time ?: emptyList()
        val dCodes = daily?.weatherCodes ?: emptyList()
        val dMaxs = daily?.temperaturesMax ?: emptyList()
        val dMins = daily?.temperaturesMin ?: emptyList()
        val dSunrises = daily?.sunrise ?: emptyList()
        val dSunsets = daily?.sunset ?: emptyList()
        val dUvs = daily?.uvIndexMax ?: emptyList()

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

        // Parse any custom warnings or build fallback empty warnings for Open-Meteo
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
            latitude = latitude,
            longitude = longitude,
            currentTemp = current?.temperature ?: 0.0,
            feelsLikeTemp = current?.apparentTemperature ?: 0.0,
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
            aqi = aqi.current?.usAqi,
            pm25 = aqi.current?.pm25,
            pm10 = aqi.current?.pm10,
            providerName = "Open-Meteo",
            alerts = alerts
        )
    }
}
