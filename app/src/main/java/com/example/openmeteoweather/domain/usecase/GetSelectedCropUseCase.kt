package com.example.openmeteoweather.domain.usecase

import com.example.openmeteoweather.domain.model.SelectedCrop
import com.example.openmeteoweather.domain.model.SelectedCropStage
import com.example.openmeteoweather.domain.repository.CropRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetSelectedCropUseCase @Inject constructor(
    private val repository: CropRepository
) {
    operator fun invoke(): Flow<Pair<SelectedCrop?, SelectedCropStage?>> {
        return combine(
            repository.getSelectedCrop(),
            repository.getSelectedCropStage()
        ) { crop, stage ->
            Pair(crop, stage)
        }
    }
}
