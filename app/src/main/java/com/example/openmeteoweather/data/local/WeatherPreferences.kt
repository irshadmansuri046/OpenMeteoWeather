package com.example.openmeteoweather.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.openmeteoweather.domain.provider.ProviderType
import com.example.openmeteoweather.domain.model.SelectedCrop
import com.example.openmeteoweather.domain.model.SelectedCropStage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weather_settings")

@Singleton
class WeatherPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val providerKey = stringPreferencesKey("selected_provider")
    
    private val cropIdKey = stringPreferencesKey("selected_crop_id")
    private val cropNameKey = stringPreferencesKey("selected_crop_name")
    private val stageIdKey = stringPreferencesKey("selected_stage_id")
    private val stageNameKey = stringPreferencesKey("selected_stage_name")

    val selectedProvider: Flow<ProviderType> = context.dataStore.data
        .map { preferences ->
            val providerName = preferences[providerKey] ?: ProviderType.OPEN_METEO.name
            try {
                ProviderType.valueOf(providerName)
            } catch (e: Exception) {
                ProviderType.OPEN_METEO
            }
        }

    val selectedCrop: Flow<SelectedCrop?> = context.dataStore.data
        .map { preferences ->
            val id = preferences[cropIdKey]
            val name = preferences[cropNameKey]
            if (id != null && name != null) {
                SelectedCrop(id, name)
            } else {
                null
            }
        }

    val selectedCropStage: Flow<SelectedCropStage?> = context.dataStore.data
        .map { preferences ->
            val id = preferences[stageIdKey]
            val name = preferences[stageNameKey]
            if (id != null && name != null) {
                SelectedCropStage(id, name)
            } else {
                null
            }
        }

    suspend fun saveSelectedProvider(provider: ProviderType) {
        context.dataStore.edit { preferences ->
            preferences[providerKey] = provider.name
        }
    }

    suspend fun saveSelectedCrop(cropId: String, cropName: String) {
        context.dataStore.edit { preferences ->
            preferences[cropIdKey] = cropId
            preferences[cropNameKey] = cropName
        }
    }

    suspend fun saveSelectedCropStage(stageId: String, stageName: String) {
        context.dataStore.edit { preferences ->
            preferences[stageIdKey] = stageId
            preferences[stageNameKey] = stageName
        }
    }

    suspend fun clearSelectedCrop() {
        context.dataStore.edit { preferences ->
            preferences.remove(cropIdKey)
            preferences.remove(cropNameKey)
            preferences.remove(stageIdKey)
            preferences.remove(stageNameKey)
        }
    }
}
