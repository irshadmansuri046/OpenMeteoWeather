package com.example.openmeteoweather.domain.usecase

import com.example.openmeteoweather.domain.model.LocationInfo
import com.example.openmeteoweather.domain.repository.WeatherRepository
import javax.inject.Inject

class SearchLocationUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(query: String): List<LocationInfo> {
        if (query.trim().isEmpty()) return emptyList()
        return repository.searchLocation(query)
    }
}
