package com.example.openmeteoweather.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Query("SELECT * FROM weather_cache WHERE ABS(latitude - :latitude) < 0.05 AND ABS(longitude - :longitude) < 0.05 LIMIT 1")
    suspend fun getWeatherCache(latitude: Double, longitude: Double): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImdWeather(weather: ImdWeatherEntity)

    @Query("SELECT * FROM imd_weather_cache WHERE ABS(latitude - :latitude) < 0.05 AND ABS(longitude - :longitude) < 0.05 LIMIT 1")
    suspend fun getImdWeatherCache(latitude: Double, longitude: Double): ImdWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAmbeeWeather(weather: AmbeeWeatherEntity)

    @Query("SELECT * FROM ambee_weather_cache WHERE ABS(latitude - :latitude) < 0.05 AND ABS(longitude - :longitude) < 0.05 LIMIT 1")
    suspend fun getAmbeeWeatherCache(latitude: Double, longitude: Double): AmbeeWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAmbeeAirQuality(aqi: AmbeeAirQualityEntity)

    @Query("SELECT * FROM ambee_air_quality_cache WHERE ABS(latitude - :latitude) < 0.05 AND ABS(longitude - :longitude) < 0.05 LIMIT 1")
    suspend fun getAmbeeAirQualityCache(latitude: Double, longitude: Double): AmbeeAirQualityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(search: SearchHistoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTomorrowWeather(weather: TomorrowWeatherEntity)

    @Query("SELECT * FROM tomorrow_weather_cache WHERE ABS(latitude - :latitude) < 0.05 AND ABS(longitude - :longitude) < 0.05 LIMIT 1")
    suspend fun getTomorrowWeatherCache(latitude: Double, longitude: Double): TomorrowWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwanWeather(weather: SwanWeatherEntity)

    @Query("SELECT * FROM swan_weather_cache WHERE ABS(latitude - :latitude) < 0.05 AND ABS(longitude - :longitude) < 0.05 LIMIT 1")
    suspend fun getSwanWeatherCache(latitude: Double, longitude: Double): SwanWeatherEntity?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpenWeather(weather: OpenWeatherWeatherEntity)

    @Query("SELECT * FROM openweather_weather_cache WHERE ABS(latitude - :latitude) < 0.05 AND ABS(longitude - :longitude) < 0.05 LIMIT 1")
    suspend fun getOpenWeatherCache(latitude: Double, longitude: Double): OpenWeatherWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOpenWeatherAlerts(alerts: OpenWeatherAlertEntity)

    @Query("SELECT * FROM openweather_alert_cache WHERE ABS(latitude - :latitude) < 0.05 AND ABS(longitude - :longitude) < 0.05 LIMIT 1")
    suspend fun getOpenWeatherAlertsCache(latitude: Double, longitude: Double): OpenWeatherAlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombinedWeather(weather: CombinedWeatherEntity)

    @Query("SELECT * FROM combined_weather_cache WHERE ABS(latitude - :latitude) < 0.05 AND ABS(longitude - :longitude) < 0.05 LIMIT 1")
    suspend fun getCombinedWeatherCache(latitude: Double, longitude: Double): CombinedWeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProviderAgreement(agreement: ProviderAgreementEntity)

    @Query("SELECT * FROM provider_agreement_cache WHERE ABS(latitude - :latitude) < 0.05 AND ABS(longitude - :longitude) < 0.05 LIMIT 1")
    suspend fun getProviderAgreementCache(latitude: Double, longitude: Double): ProviderAgreementEntity?

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Delete
    suspend fun deleteSearchHistory(search: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
