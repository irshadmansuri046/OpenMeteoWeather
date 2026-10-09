package com.example.openmeteoweather.data.repository

import com.example.openmeteoweather.data.local.CropDao
import com.example.openmeteoweather.data.local.CropEntity
import com.example.openmeteoweather.data.local.CropStageEntity
import com.example.openmeteoweather.data.local.WeatherPreferences
import com.example.openmeteoweather.domain.model.Crop
import com.example.openmeteoweather.domain.model.CropStage
import com.example.openmeteoweather.domain.model.SelectedCrop
import com.example.openmeteoweather.domain.model.SelectedCropStage
import com.example.openmeteoweather.domain.repository.CropRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

import com.example.openmeteoweather.data.remote.swan.SwanWeatherApiService

@Singleton
class CropRepositoryImpl @Inject constructor(
    private val cropDao: CropDao,
    private val preferences: WeatherPreferences,
    private val swanWeatherApiService: SwanWeatherApiService
) : CropRepository {

    // Ensure database has crops populated.
    private suspend fun ensureCropsPopulated() {
        val crops = cropDao.getAllCrops().first()
        if (crops.isEmpty() || crops.none { it.cropId == "orange" }) {
            val defaultCrops = listOf(
                CropEntity("rice", "Rice"),
                CropEntity("wheat", "Wheat"),
                CropEntity("cotton", "Cotton"),
                CropEntity("soybean", "Soybean"),
                CropEntity("sugarcane", "Sugarcane"),
                CropEntity("banana", "Banana"),
                CropEntity("grapes", "Grapes"),
                CropEntity("groundnut", "Groundnut"),
                CropEntity("maize", "Maize"),
                CropEntity("jowar", "Jowar"),
                CropEntity("ragi", "Ragi"),
                CropEntity("orange", "Orange"),
                CropEntity("paddy", "Paddy"),
                CropEntity("potato", "Potato"),
                CropEntity("onion", "Onion"),
                CropEntity("sesame", "Sesame")
            )
            cropDao.insertCrops(defaultCrops)

            val defaultStages = listOf(
                // Rice
                CropStageEntity("rice_land_prep", "rice", "Land Preparation", 1),
                CropStageEntity("rice_sowing", "rice", "Sowing", 2),
                CropStageEntity("rice_vegetative", "rice", "Vegetative", 3),
                CropStageEntity("rice_flowering", "rice", "Flowering", 4),
                CropStageEntity("rice_grain_filling", "rice", "Grain Filling", 5),
                CropStageEntity("rice_harvest", "rice", "Harvest", 6),

                // Wheat
                CropStageEntity("wheat_sowing", "wheat", "Sowing", 1),
                CropStageEntity("wheat_crown_root", "wheat", "Crown Root Initiation", 2),
                CropStageEntity("wheat_tillering", "wheat", "Tillering", 3),
                CropStageEntity("wheat_jointing", "wheat", "Jointing", 4),
                CropStageEntity("wheat_flowering", "wheat", "Flowering", 5),
                CropStageEntity("wheat_milking", "wheat", "Milking", 6),
                CropStageEntity("wheat_harvest", "wheat", "Harvest", 7),

                // Cotton
                CropStageEntity("cotton_sowing", "cotton", "Sowing", 1),
                CropStageEntity("cotton_seedling", "cotton", "Seedling", 2),
                CropStageEntity("cotton_vegetative", "cotton", "Vegetative", 3),
                CropStageEntity("cotton_flowering", "cotton", "Flowering", 4),
                CropStageEntity("cotton_boll_formation", "cotton", "Boll Formation", 5),
                CropStageEntity("cotton_harvest", "cotton", "Harvest", 6),

                // Soybean
                CropStageEntity("soybean_sowing", "soybean", "Sowing", 1),
                CropStageEntity("soybean_vegetative", "soybean", "Vegetative", 2),
                CropStageEntity("soybean_flowering", "soybean", "Flowering", 3),
                CropStageEntity("soybean_pod_development", "soybean", "Pod Development", 4),
                CropStageEntity("soybean_seed_filling", "soybean", "Seed Filling", 5),
                CropStageEntity("soybean_harvest", "soybean", "Harvest", 6),

                // Sugarcane
                CropStageEntity("sugarcane_germination", "sugarcane", "Germination", 1),
                CropStageEntity("sugarcane_tillering", "sugarcane", "Tillering", 2),
                CropStageEntity("sugarcane_grand_growth", "sugarcane", "Grand Growth", 3),
                CropStageEntity("sugarcane_maturity", "sugarcane", "Maturity", 4),
                CropStageEntity("sugarcane_harvest", "sugarcane", "Harvest", 5),

                // Banana
                CropStageEntity("banana_planting", "banana", "Planting", 1),
                CropStageEntity("banana_vegetative", "banana", "Vegetative", 2),
                CropStageEntity("banana_shooting", "banana", "Shooting", 3),
                CropStageEntity("banana_fruiting", "banana", "Fruiting", 4),
                CropStageEntity("banana_harvest", "banana", "Harvest", 5),

                // Grapes
                CropStageEntity("grapes_dormancy", "grapes", "Dormancy", 1),
                CropStageEntity("grapes_budburst", "grapes", "Budburst", 2),
                CropStageEntity("grapes_flowering", "grapes", "Flowering", 3),
                CropStageEntity("grapes_fruit_set", "grapes", "Fruit Set", 4),
                CropStageEntity("grapes_veraison", "grapes", "Veraison", 5),
                CropStageEntity("grapes_harvest", "grapes", "Harvest", 6),

                // Groundnut
                CropStageEntity("groundnut_sowing", "groundnut", "Sowing", 1),
                CropStageEntity("groundnut_vegetative", "groundnut", "Vegetative", 2),
                CropStageEntity("groundnut_flowering", "groundnut", "Flowering", 3),
                CropStageEntity("groundnut_peg_penetration", "groundnut", "Peg Penetration", 4),
                CropStageEntity("groundnut_pod_development", "groundnut", "Pod Development", 5),
                CropStageEntity("groundnut_harvest", "groundnut", "Harvest", 6),

                // Maize
                CropStageEntity("maize_sowing", "maize", "Sowing", 1),
                CropStageEntity("maize_seedling", "maize", "Seedling", 2),
                CropStageEntity("maize_vegetative", "maize", "Vegetative", 3),
                CropStageEntity("maize_tasseling", "maize", "Tasseling", 4),
                CropStageEntity("maize_silking", "maize", "Silking", 5),
                CropStageEntity("maize_grain_filling", "maize", "Grain Filling", 6),
                CropStageEntity("maize_harvest", "maize", "Harvest", 7),

                // Jowar
                CropStageEntity("jowar_sowing", "jowar", "Sowing", 1),
                CropStageEntity("jowar_seedling", "jowar", "Seedling", 2),
                CropStageEntity("jowar_vegetative", "jowar", "Vegetative", 3),
                CropStageEntity("jowar_flowering", "jowar", "Flowering", 4),
                CropStageEntity("jowar_grain_filling", "jowar", "Grain Filling", 5),
                CropStageEntity("jowar_harvest", "jowar", "Harvest", 6),

                // Ragi
                CropStageEntity("ragi_sowing", "ragi", "Sowing", 1),
                CropStageEntity("ragi_seedling", "ragi", "Seedling", 2),
                CropStageEntity("ragi_vegetative", "ragi", "Vegetative", 3),
                CropStageEntity("ragi_flowering", "ragi", "Flowering", 4),
                CropStageEntity("ragi_grain_filling", "ragi", "Grain Filling", 5),
                CropStageEntity("ragi_harvest", "ragi", "Harvest", 6),

                // Orange
                CropStageEntity("orange_flowering", "orange", "Flowering", 1),
                CropStageEntity("orange_fruit_set", "orange", "Fruit Set", 2),
                CropStageEntity("orange_fruit_development", "orange", "Fruit Development", 3),
                CropStageEntity("orange_harvest", "orange", "Harvest", 4),

                // Paddy
                CropStageEntity("paddy_land_prep", "paddy", "Land Preparation", 1),
                CropStageEntity("paddy_sowing", "paddy", "Sowing", 2),
                CropStageEntity("paddy_vegetative", "paddy", "Vegetative", 3),
                CropStageEntity("paddy_flowering", "paddy", "Flowering", 4),
                CropStageEntity("paddy_grain_filling", "paddy", "Grain Filling", 5),
                CropStageEntity("paddy_harvest", "paddy", "Harvest", 6),

                // Potato
                CropStageEntity("potato_sprouting", "potato", "Sprouting", 1),
                CropStageEntity("potato_vegetative", "potato", "Vegetative", 2),
                CropStageEntity("potato_tuber_initiation", "potato", "Tuber Initiation", 3),
                CropStageEntity("potato_tuber_bulking", "potato", "Tuber Bulking", 4),
                CropStageEntity("potato_harvest", "potato", "Harvest", 5),

                // Onion
                CropStageEntity("onion_nursery", "onion", "Nursery Stage", 1),
                CropStageEntity("onion_transplanting", "onion", "Transplanting", 2),
                CropStageEntity("onion_vegetative", "onion", "Vegetative", 3),
                CropStageEntity("onion_bulb_initiation", "onion", "Bulb Initiation", 4),
                CropStageEntity("onion_bulb_development", "onion", "Bulb Development", 5),
                CropStageEntity("onion_harvest", "onion", "Harvest", 6),

                // Sesame
                CropStageEntity("sesame_sowing", "sesame", "Sowing", 1),
                CropStageEntity("sesame_seedling", "sesame", "Seedling", 2),
                CropStageEntity("sesame_flowering", "sesame", "Flowering", 3),
                CropStageEntity("sesame_capsule_binding", "sesame", "Capsule Binding", 4),
                CropStageEntity("sesame_harvest", "sesame", "Harvest", 5)
            )
            cropDao.insertCropStages(defaultStages)
        }
    }

    override fun getCrops(): Flow<List<Crop>> = flow {
        ensureCropsPopulated()
        cropDao.getAllCrops().map { entities ->
            entities.map { Crop(it.cropId, it.cropName) }
        }.collect { emit(it) }
    }

    override fun getCropStages(cropId: String): Flow<List<CropStage>> = flow {
        ensureCropsPopulated()
        cropDao.getStagesForCrop(cropId).map { entities ->
            entities.map { CropStage(it.stageId, it.cropId, it.stageName, it.sequenceOrder) }
        }.collect { emit(it) }
    }

    override fun getSelectedCrop(): Flow<SelectedCrop?> = preferences.selectedCrop

    override fun getSelectedCropStage(): Flow<SelectedCropStage?> = preferences.selectedCropStage

    override suspend fun saveSelectedCrop(crop: Crop) {
        preferences.saveSelectedCrop(crop.id, crop.name)
        // Reset selected stage to the first stage of this crop
        val stages = cropDao.getStagesForCrop(crop.id).first()
        if (stages.isNotEmpty()) {
            val firstStage = stages.minByOrNull { it.sequenceOrder }!!
            preferences.saveSelectedCropStage(firstStage.stageId, firstStage.stageName)
        }
    }

    override suspend fun updateCropStage(stage: CropStage) {
        preferences.saveSelectedCropStage(stage.id, stage.name)
    }

    override suspend fun getCropById(cropId: String): Crop? {
        return cropDao.getCropById(cropId)?.let { Crop(it.cropId, it.cropName) }
    }

    override suspend fun getStageById(stageId: String): CropStage? {
        return cropDao.getStageById(stageId)?.let { CropStage(it.stageId, it.cropId, it.stageName, it.sequenceOrder) }
    }

    override fun getSwanCrops(): Flow<List<Crop>> = flow {
        try {
            val response = swanWeatherApiService.getCrops()
            val cropsList = response.map { Crop(it.id.toString(), it.name) }
            emit(cropsList)
        } catch (e: Exception) {
            println("SwanWeather_DEBUG: Failed to fetch Swan crops: ${e.message}")
            emit(emptyList())
        }
    }

    override fun getSwanCropStages(cropId: Int): Flow<List<CropStage>> = flow {
        try {
            val response = swanWeatherApiService.getCropStages(cropId)
            val stagesList = response.map { CropStage(it.id.toString(), it.cropId.toString(), it.stageName, it.stageOrder) }
            emit(stagesList)
        } catch (e: Exception) {
            println("SwanWeather_DEBUG: Failed to fetch Swan crop stages: ${e.message}")
            emit(emptyList())
        }
    }
}
