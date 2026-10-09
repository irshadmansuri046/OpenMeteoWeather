package com.example.openmeteoweather.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.openmeteoweather.R

@Composable
fun WeatherBackground(
    weatherCode: Int,
    content: @Composable () -> Unit
) {
    val imageRes = when (weatherCode) {
        0, 1 -> R.drawable.bg_clear
        2, 3 -> R.drawable.bg_cloudy
        45, 48 -> R.drawable.bg_cloudy
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> R.drawable.bg_rainy
        71, 73, 75, 77, 85, 86 -> R.drawable.bg_snowy
        95, 96, 99 -> R.drawable.bg_thunderstorm
        else -> R.drawable.bg_clear
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Crossfade(
            targetState = imageRes,
            animationSpec = tween(durationMillis = 1000),
            label = "WeatherBackgroundCrossfade"
        ) { targetImage ->
            Image(
                painter = painterResource(id = targetImage),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Semitransparent white overlay sheet to keep black/dark text highly legible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.35f))
        )

        content()
    }
}

