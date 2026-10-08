package com.grch.tradernettestassignment.feature.quotes.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity

/** The scrim reaches slightly below the status bar so rows fade out instead of cutting off. */
private const val SCRIM_HEIGHT_FACTOR = 1.2f
private const val SCRIM_MID_ALPHA = 0.8f

/**
 * A gradient over the status bar that keeps its icons legible while the edge-to-edge
 * list scrolls behind it. Draw it after the content.
 */
@Composable
internal fun StatusBarProtection(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.background,
) {
    val density = LocalDensity.current
    val height = with(density) { (WindowInsets.statusBars.getTop(this) * SCRIM_HEIGHT_FACTOR).toDp() }
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = SCRIM_MID_ALPHA), Color.Transparent),
                ),
            ),
    )
}
