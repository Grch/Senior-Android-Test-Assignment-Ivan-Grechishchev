package com.grch.tradernettestassignment.feature.quotes.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

private const val SHIMMER_ANIMATION_MS = 1100
private const val SHIMMER_SWEEP_WIDTH = 600f

/** A single shared, subtle shimmer brush so every pending block sweeps in sync. */
@Composable
internal fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SHIMMER_ANIMATION_MS, easing = LinearEasing),
        ),
        label = "shimmerTranslate",
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    return Brush.horizontalGradient(
        colors = listOf(base, highlight, base),
        startX = (translate - 0.5f) * SHIMMER_SWEEP_WIDTH,
        endX = (translate + 0.5f) * SHIMMER_SWEEP_WIDTH,
    )
}

/** [brush] is null only when no row is pending, so the solid fallback is just a guard. */
@Composable
internal fun ShimmerBlock(brush: Brush?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(brush ?: SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))),
    )
}
