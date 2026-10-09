package com.example.openmeteoweather.domain.model

data class CropStage(
    val id: String,
    val cropId: String,
    val name: String,
    val sequenceOrder: Int
)
