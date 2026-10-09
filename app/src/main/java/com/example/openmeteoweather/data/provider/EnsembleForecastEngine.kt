package com.example.openmeteoweather.data.provider

import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.provider.ProviderType
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class EnsembleForecastEngine @Inject constructor(
    private val openMeteoProvider: Provider<OpenMeteoProvider>,
    private val ambeeWeatherProvider: Provider<AmbeeWeatherProvider>,
    private val tomorrowWeatherProvider: Provider<TomorrowWeatherProvider>,
    private val openWeatherProvider: Provider<OpenWeatherProvider>,
    private val swanWeatherProvider: Provider<SwanWeatherProvider>
) {

    /**
     * Fetches weather forecasts in parallel from all ensemble providers.
     * Uses supervisorScope so a failure in one provider does not terminate the others.
     */
    suspend fun fetchAllProviders(latitude: Double, longitude: Double): Map<ProviderType, Result<WeatherInfo>> = supervisorScope {
        val providers = mapOf(
            ProviderType.TOMORROW_IO to tomorrowWeatherProvider,
            ProviderType.AMBEE to ambeeWeatherProvider,
            ProviderType.OPEN_WEATHER to openWeatherProvider,
            ProviderType.SWAN_WEATHER to swanWeatherProvider,
            ProviderType.OPEN_METEO to openMeteoProvider
        )

        val deferreds = providers.map { (type, provider) ->
            type to async {
                val startTime = System.currentTimeMillis()
                try {
                    val data = provider.get().getWeatherData(latitude, longitude)
                    val duration = System.currentTimeMillis() - startTime
                    Timber.d("CombinedWeather: Fetched %s successfully in %d ms", type.name, duration)
                    data
                } catch (e: Exception) {
                    val duration = System.currentTimeMillis() - startTime
                    Timber.e(e, "CombinedWeather: Failed to fetch %s in %d ms", type.name, duration)
                    throw e
                }
            }
        }

        deferreds.associate { (type, deferred) ->
            type to try {
                Result.success(deferred.await())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
