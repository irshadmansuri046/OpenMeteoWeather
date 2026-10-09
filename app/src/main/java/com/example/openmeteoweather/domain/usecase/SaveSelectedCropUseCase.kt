package com.example.openmeteoweather.domain.usecase

import com.example.openmeteoweather.domain.model.Crop
import com.example.openmeteoweather.domain.repository.CropRepository
import javax.inject.Inject

class SaveSelectedCropUseCase @Inject constructor(
    private val repository: CropRepository
) {
    suspend operator fun invoke(crop: Crop) {
        repository.saveSelectedCrop(crop)
    }
}
