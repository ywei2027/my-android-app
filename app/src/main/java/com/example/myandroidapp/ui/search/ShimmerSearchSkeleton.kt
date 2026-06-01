package com.example.myandroidapp.ui.search

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ShimmerSearchSkeleton(
    modifier: Modifier = Modifier,
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        isVisible = true
    }

    if (!isVisible) return

    Column(modifier = modifier.padding(16.dp)) {
        ShimmerBlock(
            widthFraction = 0.85f,
            height = 16.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBlock(
            widthFraction = 0.5f,
            height = 12.dp,
            modifier = Modifier.fillMaxWidth(0.6f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        ShimmerBlock(
            widthFraction = 0.9f,
            height = 16.dp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBlock(
            widthFraction = 0.4f,
            height = 12.dp,
            modifier = Modifier.fillMaxWidth(0.5f),
        )
    }
}

@Composable
private fun ShimmerBlock(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerTranslate",
    )

    val shimmerColors = listOf(
        Color(0xFFE7E0EC),
        Color(0xFFF5F2F7),
        Color(0xFFE7E0EC),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f),
    )

    Box(
        modifier = modifier
            .height(height)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .background(brush),
    )
}
