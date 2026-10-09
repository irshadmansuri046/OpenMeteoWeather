package com.example.openmeteoweather.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerPlaceholder(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .background(brush = brush, shape = shape)
    )
}

@Composable
fun HomeWeatherShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search bar shimmer
        ShimmerPlaceholder(width = 300.dp, height = 50.dp, shape = RoundedCornerShape(24.dp))
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Large Temperature Card Shimmer
        ShimmerPlaceholder(width = 320.dp, height = 180.dp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Advisories Header Shimmer
        ShimmerPlaceholder(width = 180.dp, height = 24.dp)
        
        // Advisory Cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ShimmerPlaceholder(width = 150.dp, height = 120.dp)
            ShimmerPlaceholder(width = 150.dp, height = 120.dp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Hourly Weather Section
        ShimmerPlaceholder(width = 120.dp, height = 24.dp)
        ShimmerPlaceholder(width = 320.dp, height = 110.dp)
    }
}
