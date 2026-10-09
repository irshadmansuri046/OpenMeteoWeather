package com.example.openmeteoweather.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.openmeteoweather.domain.location.LocationTracker
import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.usecase.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.openmeteoweather.domain.provider.ProviderType
import com.example.openmeteoweather.data.provider.WeatherProviderManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.openmeteoweather.domain.model.Crop
import com.example.openmeteoweather.domain.model.CropStage
import com.example.openmeteoweather.domain.model.SelectedCrop
import com.example.openmeteoweather.domain.model.SelectedCropStage
import com.example.openmeteoweather.domain.repository.CropRepository
import com.example.openmeteoweather.domain.usecase.GetSelectedCropUseCase
import com.example.openmeteoweather.domain.usecase.SaveSelectedCropUseCase
import com.example.openmeteoweather.domain.usecase.UpdateCropStageUseCase

data class WeatherState(
    val weatherInfo: WeatherInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getWeatherUseCase: GetWeatherUseCase,
    private val locationTracker: LocationTracker,
    private val providerManager: WeatherProviderManager,
    private val cropRepository: CropRepository,
    private val getSelectedCropUseCase: GetSelectedCropUseCase,
    private val saveSelectedCropUseCase: SaveSelectedCropUseCase,
    private val updateCropStageUseCase: UpdateCropStageUseCase
) : ViewModel() {

    val selectedProvider = providerManager.selectedProvider
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProviderType.OPEN_METEO
        )

    // Expose all crops dynamically based on provider
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allCrops: StateFlow<List<Crop>> = selectedProvider
        .flatMapLatest { provider ->
            if (provider == ProviderType.SWAN_WEATHER) {
                cropRepository.getSwanCrops()
            } else {
                cropRepository.getCrops()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Expose selected crop and stage combined selection flow
    val cropSelection: StateFlow<Pair<SelectedCrop?, SelectedCropStage?>> = getSelectedCropUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Pair(null, null)
        )

    // Helper to fetch stages dynamically for a selected crop id based on provider
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getStagesForCrop(cropId: String): Flow<List<CropStage>> {
        return selectedProvider.flatMapLatest { provider ->
            if (provider == ProviderType.SWAN_WEATHER) {
                val idInt = cropId.toIntOrNull() ?: 0
                cropRepository.getSwanCropStages(idInt)
            } else {
                cropRepository.getCropStages(cropId)
            }
        }
    }

    fun selectCrop(crop: Crop) {
        viewModelScope.launch {
            saveSelectedCropUseCase(crop)
        }
    }

    fun selectStage(stage: CropStage) {
        viewModelScope.launch {
            updateCropStageUseCase(stage)
        }
    }

    private val _state = MutableStateFlow(WeatherState())
    val state = _state.asStateFlow()

    private val _cityName = MutableStateFlow("Current Location")
    val cityName = _cityName.asStateFlow()


    init {
        viewModelScope.launch {
            providerManager.selectedProvider
                .drop(1)
                .collect {
                    loadWeatherInfo(
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        name = _cityName.value,
                        forceRefresh = false
                    )
                }
        }
    }

    // Default: Mumbai coordinates
    private var currentLatitude: Double = 19.0760
    private var currentLongitude: Double = 72.8777

    fun loadWeatherInfo(
        latitude: Double? = null,
        longitude: Double? = null,
        name: String? = null,
        forceRefresh: Boolean = false
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            // Determine coordinate resolution
            if (latitude != null && longitude != null) {
                currentLatitude = latitude
                currentLongitude = longitude
                _cityName.value = name ?: "Selected Location"
                fetchWeather(currentLatitude, currentLongitude, forceRefresh)
            } else {
                // Fetch current device GPS location
                val location = locationTracker.getCurrentLocation()
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    _cityName.value = "Current Location"
                    fetchWeather(currentLatitude, currentLongitude, forceRefresh)
                } else {
                    // Could not fetch GPS location. Try to check if we have ANY cache
                    // or fall back to default Mumbai weather
                    _cityName.value = name ?: "Mumbai (Default)"
                    fetchWeather(currentLatitude, currentLongitude, forceRefresh)
                }
            }
        }
    }

    private suspend fun fetchWeather(lat: Double, lng: Double, forceRefresh: Boolean) {
        try {
            val result = getWeatherUseCase(lat, lng, forceRefresh)
            _state.value = _state.value.copy(
                weatherInfo = result,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                weatherInfo = null,
                isLoading = false,
                error = e.message ?: "An unexpected network or database error occurred."
            )
        }
    }

    fun refreshWeather() {
        loadWeatherInfo(
            latitude = currentLatitude,
            longitude = currentLongitude,
            name = _cityName.value,
            forceRefresh = true
        )
    }

    fun updateWeatherProvider(provider: ProviderType) {
        viewModelScope.launch {
            providerManager.updateProviderSetting(provider)
        }
    }
}
