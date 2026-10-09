package com.example.openmeteoweather.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CropDao {
    @Query("SELECT * FROM crops")
    fun getAllCrops(): Flow<List<CropEntity>>

    @Query("SELECT * FROM crop_stages WHERE cropId = :cropId ORDER BY sequenceOrder ASC")
    fun getStagesForCrop(cropId: String): Flow<List<CropStageEntity>>

    @Query("SELECT * FROM crops WHERE cropId = :cropId LIMIT 1")
    suspend fun getCropById(cropId: String): CropEntity?

    @Query("SELECT * FROM crop_stages WHERE stageId = :stageId LIMIT 1")
    suspend fun getStageById(stageId: String): CropStageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrops(crops: List<CropEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCropStages(stages: List<CropStageEntity>)
}
