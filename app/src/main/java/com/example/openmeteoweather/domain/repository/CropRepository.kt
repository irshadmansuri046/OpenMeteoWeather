package com.example.openmeteoweather.domain.repository

import com.example.openmeteoweather.domain.model.Crop
import com.example.openmeteoweather.domain.model.CropStage
import com.example.openmeteoweather.domain.model.SelectedCrop
import com.example.openmeteoweather.domain.model.SelectedCropStage
import kotlinx.coroutines.flow.Flow

interface CropRepository {
    fun getCrops(): Flow<List<Crop>>
    fun getCropStages(cropId: String): Flow<List<CropStage>>
    fun getSwanCrops(): Flow<List<Crop>>
    fun getSwanCropStages(cropId: Int): Flow<List<CropStage>>
    
    fun getSelectedCrop(): Flow<SelectedCrop?>
    fun getSelectedCropStage(): Flow<SelectedCropStage?>
    
    suspend fun saveSelectedCrop(crop: Crop)
    suspend fun updateCropStage(stage: CropStage)
    
    suspend fun getCropById(cropId: String): Crop?
    suspend fun getStageById(stageId: String): CropStage?
}
