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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openmeteoweather.domain.model.Advisory
import com.example.openmeteoweather.domain.model.AdvisoryType
import com.example.openmeteoweather.theme.*

@Composable
fun AdvisoryCard(
    advisory: Advisory,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, iconColor) = when (advisory.type) {
        AdvisoryType.GOOD -> Quadruple(AdvisoryGood, AdvisoryGoodText, Icons.Rounded.CheckCircle, AdvisoryGoodText)
        AdvisoryType.INFO -> Quadruple(AdvisoryInfo, AdvisoryInfoText, Icons.Rounded.Info, AdvisoryInfoText)
        AdvisoryType.WARNING -> Quadruple(AdvisoryWarning, AdvisoryWarningText, Icons.Rounded.Warning, AdvisoryWarningText)
        AdvisoryType.DANGER -> Quadruple(AdvisoryDanger, AdvisoryDangerText, Icons.Rounded.Error, AdvisoryDangerText)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, iconColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = advisory.title,
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = advisory.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                )

                // Render Crop & Stage Badges if present
                if (advisory.cropName != null && advisory.stageName != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgeChip(
                            icon = Icons.Rounded.Eco,
                            text = "Crop: ${advisory.cropName}",
                            textColor = textColor,
                            bgColor = iconColor.copy(alpha = 0.1f)
                        )
                        BadgeChip(
                            icon = Icons.Rounded.Grass,
                            text = "Stage: ${advisory.stageName}",
                            textColor = textColor,
                            bgColor = iconColor.copy(alpha = 0.1f)
                        )
                    }
                }

                // Main Message or structured Reason / Action
                if (advisory.reason != null || advisory.action != null) {
                    if (advisory.reason != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Reason:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = textColor.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = advisory.reason,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = textColor.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }

                    if (advisory.action != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Action:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = textColor.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = advisory.action,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                } else {
                    // Fallback to simple message
                    Text(
                        text = advisory.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = textColor.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    )
                }

                // Render Severity Text Badge if structured
                if (advisory.cropName != null) {
                    val severityText = when (advisory.type) {
                        AdvisoryType.GOOD -> "Favorable"
                        AdvisoryType.INFO -> "Info"
                        AdvisoryType.WARNING -> "Warning"
                        AdvisoryType.DANGER -> "Danger"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Severity: $severityText",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = textColor.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textColor: Color,
    bgColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.8f),
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
            )
        )
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
