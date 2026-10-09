package com.example.openmeteoweather.data.remote.swan

import com.google.gson.annotations.SerializedName

data class SwanWeatherResponse(
    val status: Int,
    val message: String?,
    val data: SwanCustomWeatherData?
)

data class SwanCropDto(
    val id: Int,
    val name: String,
    val category: String?
)

data class SwanCropStageDto(
    val id: Int,
    @SerializedName("crop_id") val cropId: Int,
    @SerializedName("stage_name") val stageName: String,
    @SerializedName("stage_order") val stageOrder: Int,
    val description: String?
)

data class SwanCustomWeatherRequest(
    val latitude: Double,
    val longitude: Double,
    @SerializedName("crop_stages") val cropStages: List<SwanCropStageQuery>
)

data class SwanCropStageQuery(
    @SerializedName("crop_id") val cropId: Int,
    @SerializedName("stage_id") val stageId: Int
)

data class SwanCustomWeatherResponse(
    val status: Int,
    val message: String?,
    val data: SwanCustomWeatherData?,
    val advisories: List<SwanDailyAdvisoryDto>?
)

data class SwanDailyAdvisoryDto(
    val date: String?,
    @SerializedName("crop_name") val cropName: String?,
    @SerializedName("stage_name") val stageName: String?,
    val alerts: List<SwanAlertDto>?
)

data class SwanAlertDto(
    @SerializedName("rule_id") val ruleId: Int?,
    @SerializedName("rule_name") val ruleName: String?,
    val parameter: String?,
    @SerializedName("actual_value") val actualValue: String?,
    @SerializedName("threshold_value") val thresholdValue: String?,
    val operator: String?,
    val severity: String?,
    @SerializedName("advisory_text") val advisoryText: String?
)

data class SwanCustomWeatherData(
    val daily: List<SwanParameterData>?,
    val weekly: List<SwanParameterData>?,
    val common: List<SwanParameterData>?
)

data class SwanParameterData(
    val parameter: String?,
    @SerializedName("raw_key") val rawKey: String?,
    val height: String?,
    val unit: String?,
    val label: String?,
    val coordinates: List<SwanCoordinateData>?
)

data class SwanCoordinateData(
    val lat: Double?,
    val lon: Double?,
    val dates: List<SwanDateValue>?
)

data class SwanDateValue(
    val date: String?,
    val value: String?
)

data class SwanAdvisoryDto(
    val id: Int?,
    @SerializedName("crop_id") val cropId: Int?,
    @SerializedName("stage_id") val stageId: Int?,
    @SerializedName("crop_name") val cropName: String?,
    @SerializedName("stage_name") val stageName: String?,
    val severity: String?,
    val title: String?,
    val message: String?,
    val advisory: String?,
    val reason: String?,
    val action: String?,
    val timing: String?
)
