package com.example.openmeteoweather.domain.model

enum class AdvisoryType {
    GOOD,     // Favorable conditions for farming activities
    INFO,     // General information or warning to be cautious
    WARNING,  // Conditions that could harm crops or affect applications
    DANGER    // High risk, immediate action required
}

data class Advisory(
    val title: String,
    val message: String,
    val type: AdvisoryType,
    val cropName: String? = null,
    val stageName: String? = null,
    val reason: String? = null,
    val action: String? = null
)

