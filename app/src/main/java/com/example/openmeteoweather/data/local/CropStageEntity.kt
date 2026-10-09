package com.example.openmeteoweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "crop_stages")
data class CropStageEntity(
    @PrimaryKey val stageId: String,
    val cropId: String,
    val stageName: String,
    val sequenceOrder: Int
)
