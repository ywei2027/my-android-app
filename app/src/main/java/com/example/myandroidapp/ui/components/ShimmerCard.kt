package com.example.myandroidapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * 骨架屏卡片 — Row 布局与 NewsCard 一致：
 *   Row[缩略图占位 80×60dp | Column[标题+副标题+描述占位]]
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier,
    testTag: String = "shimmerCard"
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = NewsDimens.CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = NewsDimens.CardElevation)
    ) {
        Row(modifier = Modifier.padding(NewsDimens.CardPadding)) {
            // 左侧缩略图占位 80×60dp
            Box(
                modifier = Modifier
                    .width(NewsDimens.ShimmerThumbnailWidth)
                    .height(NewsDimens.ShimmerThumbnailHeight)
                    .clip(NewsDimens.ThumbnailShape)
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧文字占位
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(NewsDimens.ShimmerTitleHeight)
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(NewsDimens.CardSpacing))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(NewsDimens.ShimmerSubtitleHeight)
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(NewsDimens.CardSpacing))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(NewsDimens.ShimmerBodyHeight)
                        .background(shimmerBrush)
                )
            }
        }
    }
}
