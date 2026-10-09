package com.example.openmeteoweather.domain.usecase

import com.example.openmeteoweather.domain.model.LocationInfo
import com.example.openmeteoweather.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentSearchesUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    operator fun invoke(): Flow<List<LocationInfo>> {
        return repository.getRecentSearches()
    }
}
