package com.example.openmeteoweather.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "provider_agreement_cache")
data class ProviderAgreementEntity(
    @PrimaryKey
    val id: String, // E.g., "lat,lng"
    val latitude: Double,
    val longitude: Double,
    val agreementPercentage: Double,
    val confidenceScore: Double,
    val confidenceLevel: String,
    val rawComparisonJson: String,
    val lastUpdated: Long
)
