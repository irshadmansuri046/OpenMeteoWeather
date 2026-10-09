package com.example.openmeteoweather.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openmeteoweather.domain.model.WeatherInfo
import com.example.openmeteoweather.domain.util.formatDecimal

@Composable
fun WeatherMetricsGrid(
    weather: WeatherInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Humidity",
                value = "${weather.humidity.formatDecimal()}%",
                icon = Icons.Rounded.WaterDrop,
                iconColor = Color(0xFF64B5F6),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Wind Speed",
                value = "${weather.windSpeed.formatDecimal()} km/h",
                icon = Icons.Rounded.Air,
                iconColor = Color(0xFFB0BEC5),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Rain Prob",
                value = "${weather.rainProbability}%",
                icon = Icons.Rounded.Umbrella,
                iconColor = Color(0xFF81C784),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "UV Index",
                value = weather.uvIndex.formatDecimal(),
                subtitle = getUvDescription(weather.uvIndex),
                icon = Icons.Rounded.WbSunny,
                iconColor = Color(0xFFFFD54F),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Sunrise",
                value = formatTime(weather.sunrise),
                icon = Icons.Rounded.LightMode,
                iconColor = Color(0xFFFFB74D),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Sunset",
                value = formatTime(weather.sunset),
                icon = Icons.Rounded.WbTwilight,
                iconColor = Color(0xFFAB47BC),
                modifier = Modifier.weight(1f)
            )
        }
        if (weather.aqi != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Air Quality (AQI)",
                    value = "${weather.aqi}",
                    subtitle = getAqiDescription(weather.aqi),
                    icon = Icons.Rounded.Co2,
                    iconColor = getAqiColor(weather.aqi),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    // Glassmorphic design
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color.Black.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = Color.Black,
                    fontSize = 20.sp
                )
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Black.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

private fun getUvDescription(uv: Double): String {
    return when {
        uv < 3 -> "Low"
        uv < 6 -> "Moderate"
        uv < 8 -> "High"
        uv < 11 -> "Very High"
        else -> "Extreme"
    }
}

private fun getAqiDescription(aqi: Int): String {
    return when {
        aqi <= 50 -> "Good"
        aqi <= 100 -> "Moderate"
        aqi <= 150 -> "Unhealthy (Sensitive)"
        aqi <= 200 -> "Unhealthy"
        aqi <= 300 -> "Very Unhealthy"
        else -> "Hazardous"
    }
}

private fun getAqiColor(aqi: Int): Color {
    return when {
        aqi <= 50 -> Color(0xFF81C784)
        aqi <= 100 -> Color(0xFFFFD54F)
        aqi <= 150 -> Color(0xFFFFB74D)
        aqi <= 200 -> Color(0xFFE57373)
        aqi <= 300 -> Color(0xFFBA68C8)
        else -> Color(0xFFD32F2F)
    }
}

private fun formatTime(dateTimeStr: String): String {
    // Open-Meteo returns ISO strings, e.g. "2026-05-25T06:05"
    // Let's parse and return HH:MM
    return try {
        val parts = dateTimeStr.split("T")
        if (parts.size == 2) {
            parts[1]
        } else {
            dateTimeStr
        }
    } catch (e: Exception) {
        dateTimeStr
    }
}
