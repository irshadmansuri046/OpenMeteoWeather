package com.example.openmeteoweather.domain.model

data class AdvisoryContext(
    val weather: WeatherInfo,
    val crop: Crop,
    val stage: CropStage
)
