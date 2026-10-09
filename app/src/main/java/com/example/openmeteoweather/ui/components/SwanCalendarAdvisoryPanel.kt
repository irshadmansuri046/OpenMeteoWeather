package com.example.openmeteoweather.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openmeteoweather.data.remote.swan.SwanAlertDto
import com.example.openmeteoweather.data.remote.swan.SwanDailyAdvisoryDto
import com.example.openmeteoweather.theme.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SwanCalendarAdvisoryPanel(
    customAdvisoriesJson: String,
    modifier: Modifier = Modifier
) {
    val dailyAdvisories = remember(customAdvisoriesJson) {
        try {
            val listType = object : TypeToken<List<SwanDailyAdvisoryDto>>() {}.type
            val list: List<SwanDailyAdvisoryDto> = Gson().fromJson(customAdvisoriesJson, listType)
            // Limit to a week (up to 7 days)
            list.take(7)
        } catch (e: Exception) {
            emptyList()
        }
    }

    if (dailyAdvisories.isEmpty()) return

    // Track selected date
    var selectedDateStr by remember { mutableStateOf(dailyAdvisories.firstOrNull()?.date ?: "") }
    
    // Ensure selected date is valid if JSON changed
    LaunchedEffect(dailyAdvisories) {
        if (dailyAdvisories.none { it.date == selectedDateStr }) {
            selectedDateStr = dailyAdvisories.firstOrNull()?.date ?: ""
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SwanWeather Crop Advisories",
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        )

        // 1. Horizontal Weekly Calendar Control
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(dailyAdvisories) { advisory ->
                val dateStr = advisory.date ?: ""
                val isSelected = dateStr == selectedDateStr
                val dateParts = formatAdvisoryDate(dateStr)
                val hasAlerts = !advisory.alerts.isNullOrEmpty()

                val cardBg = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                } else {
                    Color.White
                }
                val borderCol = if (isSelected) {
                    Color.Transparent
                } else {
                    Color.LightGray.copy(alpha = 0.6f)
                }
                val textCol = if (isSelected) Color.White else Color.DarkGray
                val dateNumCol = if (isSelected) Color.White else Color.Black

                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .height(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                        .clickable { selectedDateStr = dateStr }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        // Weekday (e.g. Sat)
                        Text(
                            text = dateParts.first,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = textCol.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        )
                        // Day Number (e.g. 6)
                        Text(
                            text = dateParts.second,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = dateNumCol,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                        // Alert Indicator Dot
                        if (hasAlerts) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else Color.Red)
                            )
                        } else {
                            Spacer(modifier = Modifier.size(6.dp))
                        }
                    }
                }
            }
        }

        // 2. Crop & Stage Info Bar
        val selectedAdvisory = dailyAdvisories.find { it.date == selectedDateStr }
        if (selectedAdvisory != null && (!selectedAdvisory.cropName.isNullOrEmpty() || !selectedAdvisory.stageName.isNullOrEmpty())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Eco,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${selectedAdvisory.cropName ?: "Unknown Crop"} (${selectedAdvisory.stageName ?: "Unknown Stage"})",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                )
            }
        }

        // 3. Animated Transitions for Advisories
        Crossfade(
            targetState = selectedDateStr,
            animationSpec = tween(durationMillis = 350),
            label = "advisory_date_change"
        ) { targetDate ->
            val advisoryForDate = dailyAdvisories.find { it.date == targetDate }
            val alerts = advisoryForDate?.alerts ?: emptyList()

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (alerts.isEmpty()) {
                    // Clean state
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AdvisoryGood),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AdvisoryGoodText.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Favorable weather",
                                tint = AdvisoryGoodText,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Safe Farming Day",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = AdvisoryGoodText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                    text = "No weather danger for this day. You can safely spray, irrigate, or harvest as per your regular routine.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = AdvisoryGoodText.copy(alpha = 0.85f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }
                    }
                } else {
                    alerts.forEach { alert ->
                        SwanAlertCard(alert = alert)
                    }
                }
            }
        }
    }
}

@Composable
fun SwanAlertCard(alert: SwanAlertDto) {
    val parameter = alert.parameter?.lowercase()
    
    // Determine the unique card design (Background, Text Color, Icon, Icon Color, Badge Text)
    val (bgColor, textColor, icon, iconColor, badgeText) = when (parameter) {
        "precipitation", "rain" -> Quintuple(
            Color(0xFFE3F2FD), // Light Blue
            Color(0xFF0D47A1), // Deep Blue
            Icons.Rounded.WaterDrop,
            Color(0xFF1976D2), // Rain Icon Color
            "💧 Rainfall Alert"
        )
        "temp_max", "max_temperature" -> Quintuple(
            Color(0xFFFFF3E0), // Light Orange
            Color(0xFFE65100), // Deep Orange
            Icons.Rounded.WbSunny,
            Color(0xFFF57C00), // Heat Icon Color
            "☀️ High Heat Alert"
        )
        "temp_min", "min_temperature" -> Quintuple(
            Color(0xFFE0F2F1), // Light Teal
            Color(0xFF004D40), // Deep Teal
            Icons.Rounded.AcUnit,
            Color(0xFF00796B), // Cold Icon Color
            "❄️ Cold / Frost Alert"
        )
        "humidity_avg", "humidity" -> Quintuple(
            Color(0xFFF3E5F5), // Light Purple
            Color(0xFF4A148C), // Deep Purple
            Icons.Rounded.Opacity,
            Color(0xFF7B1FA2), // Damp Icon Color
            "🌫️ Damp Air Warning"
        )
        "wind_speed_max", "wind_speed", "wind" -> Quintuple(
            Color(0xFFECEFF1), // Light Slate Grey
            Color(0xFF263238), // Deep Slate
            Icons.Rounded.Air,
            Color(0xFF455A64), // Wind Icon Color
            "💨 Strong Winds"
        )
        else -> Quintuple(
            Color(0xFFFFFDE7), // Light Yellow Default
            Color(0xFF5D4037), // Brown
            Icons.Rounded.Info,
            Color(0xFFFBC02D), // General Icon Color
            "ℹ️ Farm Alert"
        )
    }

    val friendlyComparison = getFarmerFriendlyComparison(alert)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, iconColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Parameter Icon with clean circular border
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = alert.parameter,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Custom Badge Text instead of raw rule name
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    
                    Text(
                        text = alert.ruleName ?: "Weather Warning",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = textColor.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    )
                }

                // Severity Badge
                val severity = alert.severity?.uppercase()
                val isDanger = severity == "DANGER" || severity == "CRITICAL" || severity == "HIGH"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(iconColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isDanger) "DANGER" else severity ?: "WARNING",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = iconColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            // Description block for the comparison
            Text(
                text = friendlyComparison,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            )

            // Action / Advisory Text message
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.5f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Text(
                    text = alert.advisoryText ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatAdvisoryDate(dateStr: String): Pair<String, String> {
    return try {
        val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdfInput.parse(dateStr)
        if (date != null) {
            val sdfDay = SimpleDateFormat("EEE", Locale.US) // e.g. "Sat"
            val sdfNum = SimpleDateFormat("d", Locale.US)   // e.g. "6"
            Pair(sdfDay.format(date), sdfNum.format(date))
        } else {
            Pair("", "")
        }
    } catch (e: Exception) {
        Pair("", "")
    }
}

private fun getParameterIcon(parameter: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (parameter?.lowercase()) {
        "precipitation", "rain", "rain_probability", "precipitation_probability" -> Icons.Rounded.WaterDrop
        "humidity_avg", "relative_humidity", "humidity", "relative_humidity_min" -> Icons.Rounded.Opacity
        "temperature", "temp", "temp_max", "temp_min", "min_temperature", "max_temperature", "temperature_mean" -> Icons.Rounded.Thermostat
        "wind_speed", "wind_gusts", "wind", "wind_speed_max" -> Icons.Rounded.Air
        else -> Icons.Rounded.Info
    }
}

private fun getFarmerFriendlyComparison(alert: SwanAlertDto): String {
    val actualStr = alert.actualValue ?: "0.0"
    val thresholdStr = alert.thresholdValue ?: "0.0"
    val actualDouble = actualStr.toDoubleOrNull() ?: 0.0
    val thresholdDouble = thresholdStr.toDoubleOrNull() ?: 0.0
    
    return when (alert.parameter?.lowercase()) {
        "precipitation", "rain" -> {
            val rainIntensity = if (actualDouble > 25.0) "heavy shower" else "light rain"
            "Expected rain is ${actualStr} mm ($rainIntensity), which is more than what the crop can handle (limit: ${thresholdStr} mm)."
        }
        "humidity_avg", "humidity" -> {
            "Air is very damp (${actualStr}%), exceeding the safe limit of ${thresholdStr}% (which increases disease risk)."
        }
        "temp_max", "max_temperature" -> {
            "Heat level will reach ${actualStr}°C, which is hotter than the safe limit of ${thresholdStr}°C."
        }
        "temp_min", "min_temperature" -> {
            "Cold level will drop to ${actualStr}°C, which is colder than the safe limit of ${thresholdStr}°C."
        }
        "wind_speed_max", "wind_speed", "wind" -> {
            "Wind will blow up to ${actualStr} m/s, which is stronger than the safe speed of ${thresholdStr} m/s."
        }
        else -> {
            "Current level is ${actualStr}, exceeding the safe limit of ${thresholdStr}."
        }
    }
}

private data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
