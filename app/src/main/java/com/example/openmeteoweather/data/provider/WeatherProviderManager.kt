package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.data.local.WeatherPreferences
import com.example.openmeteoweather.domain.provider.ProviderType
import com.example.openmeteoweather.domain.provider.WeatherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class WeatherProviderManager @Inject constructor(
    private val openMeteoProvider: Provider<OpenMeteoProvider>,
    private val imdWeatherProvider: Provider<ImdWeatherProvider>,
    private val ambeeWeatherProvider: Provider<AmbeeWeatherProvider>,
    private val tomorrowWeatherProvider: Provider<TomorrowWeatherProvider>,
    private val openWeatherProvider: Provider<OpenWeatherProvider>,
    private val swanWeatherProvider: Provider<SwanWeatherProvider>,
    private val combinedWeatherProvider: Provider<CombinedWeatherProvider>,
    private val preferences: WeatherPreferences
) {
    val selectedProvider: Flow<ProviderType> = preferences.selectedProvider

    suspend fun getActiveProvider(): WeatherProvider {
        val type = selectedProvider.first()
        return getProvider(type)
    }

    fun getProvider(type: ProviderType): WeatherProvider {
        return when (type) {
            ProviderType.OPEN_METEO -> openMeteoProvider.get()
            ProviderType.IMD -> imdWeatherProvider.get()
            ProviderType.AMBEE -> ambeeWeatherProvider.get()
            ProviderType.TOMORROW_IO -> tomorrowWeatherProvider.get()
            ProviderType.OPEN_WEATHER -> openWeatherProvider.get()
            ProviderType.SWAN_WEATHER -> swanWeatherProvider.get()
            ProviderType.COMBINED_WEATHER -> combinedWeatherProvider.get()
        }
    }

    suspend fun getActiveProviderType(): ProviderType {
        return selectedProvider.first()
    }

    suspend fun updateProviderSetting(provider: ProviderType) {
        preferences.saveSelectedProvider(provider)
    }
}
