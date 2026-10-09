package com.example.openmeteoweather.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.openmeteoweather.theme.*

object WeatherCodeUtils {

    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45 -> "Foggy"
            48 -> "Depositing Rime Fog"
            51 -> "Light Drizzle"
            53 -> "Moderate Drizzle"
            55 -> "Dense Drizzle"
            56 -> "Light Freezing Drizzle"
            57 -> "Dense Freezing Drizzle"
            61 -> "Slight Rain"
            63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            66 -> "Light Freezing Rain"
            67 -> "Heavy Freezing Rain"
            71 -> "Slight Snowfall"
            73 -> "Moderate Snowfall"
            75 -> "Heavy Snowfall"
            77 -> "Snow Grains"
            80 -> "Slight Rain Showers"
            81 -> "Moderate Rain Showers"
            82 -> "Violent Rain Showers"
            85 -> "Slight Snow Showers"
            86 -> "Heavy Snow Showers"
            95 -> "Thunderstorm"
            96 -> "Thunderstorm with Slight Hail"
            99 -> "Thunderstorm with Heavy Hail"
            else -> "Unknown Weather"
        }
    }

    fun getWeatherIcon(code: Int): ImageVector {
        return when (code) {
            0, 1 -> Icons.Rounded.WbSunny
            2 -> Icons.Rounded.CloudQueue
            3 -> Icons.Rounded.Cloud
            45, 48 -> Icons.Rounded.FilterDrama // Representing fog
            51, 53, 55, 56, 57 -> Icons.Rounded.WaterDrop
            61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Rounded.Grain
            71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
            95, 96, 99 -> Icons.Rounded.Thunderstorm
            else -> Icons.Rounded.QuestionMark
        }
    }

    fun getWeatherGradients(code: Int): List<Color> {
        return when (code) {
            0, 1 -> listOf(ClearDayStart, ClearDayEnd)
            2, 3 -> listOf(CloudyStart, CloudyEnd)
            45, 48 -> listOf(CloudyStart, Color(0xFF90A4AE))
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> listOf(RainStart, RainEnd)
            71, 73, 75, 77, 85, 86 -> listOf(Color(0xFFE0F7FA), Color(0xFF80DEEA))
            95, 96, 99 -> listOf(Color(0xFF263238), Color(0xFF455A64))
            else -> listOf(ClearDayStart, ClearDayEnd)
        }
    }
}
