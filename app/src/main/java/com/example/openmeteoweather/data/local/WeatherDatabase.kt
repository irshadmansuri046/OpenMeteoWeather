package com.example.openmeteoweather.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WeatherEntity::class,
        ImdWeatherEntity::class,
        AmbeeWeatherEntity::class,
        AmbeeAirQualityEntity::class,
        SearchHistoryEntity::class,
        TomorrowWeatherEntity::class,
        OpenWeatherWeatherEntity::class,
        OpenWeatherAlertEntity::class,
        CombinedWeatherEntity::class,
        ProviderAgreementEntity::class,
        CropEntity::class,
        CropStageEntity::class,
        SwanWeatherEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class WeatherDatabase : RoomDatabase() {
    abstract val dao: WeatherDao
    abstract val cropDao: CropDao
}

