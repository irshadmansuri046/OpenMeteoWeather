package com.example.openmeteoweather.domain.usecase

import com.example.openmeteoweather.domain.model.LocationInfo
import com.example.openmeteoweather.domain.repository.WeatherRepository
import javax.inject.Inject

class SaveRecentSearchUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(location: LocationInfo) {
        repository.saveRecentSearch(location)
    }
}
