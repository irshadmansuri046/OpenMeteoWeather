package com.example.openmeteoweather.domain.usecase

import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.repository.WeatherRepository
import javax.inject.Inject

class GetWeatherUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(
        latitude: Double,
        longitude: Double,
        forceRefresh: Boolean = false
    ): WeatherInfo {
        return repository.getWeatherData(latitude, longitude, forceRefresh)
    }
}
