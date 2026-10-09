package com.example.openmeteoweather.domain.usecase

import com.example.openmeteoweather.domain.model.CropStage
import com.example.openmeteoweather.domain.repository.CropRepository
import javax.inject.Inject

class UpdateCropStageUseCase @Inject constructor(
    private val repository: CropRepository
) {
    suspend operator fun invoke(stage: CropStage) {
        repository.updateCropStage(stage)
    }
}
