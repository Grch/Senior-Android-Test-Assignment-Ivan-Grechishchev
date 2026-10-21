package com.grch.tradernettestassignment.feature.quotes.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val FLASH_DURATION_MS = 300L

/** Lining-digit height as a fraction of the font size (Roboto cap/digit height is about 0.71-0.72 em). */
private const val DIGIT_HEIGHT_EM = 0.72f

/** Outset of the flash pill around the glyphs. */
private val PillHorizontalPadding = 2.dp
private val PillVerticalPadding = 4.dp

/** Test-only semantics flag: true while the percent badge shows its tick highlight. */
internal val PercentHighlighted = SemanticsPropertyKey<Boolean>("PercentHighlighted")
private var SemanticsPropertyReceiver.percentHighlighted by PercentHighlighted

/** The percent change; on a tick it shows a green/red pill with white text for [FLASH_DURATION_MS]. */
@Composable
internal fun PercentBadge(
    text: String,
    sign: Double?,
    flash: FlashDirection,
    flashTick: Int,
    modifier: Modifier = Modifier,
) {
    val firstSeenTick = remember { flashTick } // don't flash a row just because it (re)entered composition
    var highlighted by remember { mutableStateOf(false) }
    // A new tick changes the key, so Compose restarts the highlight timer.
    LaunchedEffect(flashTick) {
        if (flashTick == firstSeenTick) {
            highlighted = false
            return@LaunchedEffect
        }
        highlighted = true
        delay(FLASH_DURATION_MS)
        highlighted = false
    }
    val background = if (!highlighted) {
        Color.Transparent
    } else when (flash) {
        FlashDirection.UP -> PositiveGreen
        FlashDirection.DOWN -> NegativeRed
        FlashDirection.NONE -> Color.Transparent
    }
    val textColor = if (highlighted) Color.White else signColor(sign)
    val textStyle = MaterialTheme.typography.titleMedium
    // Read only while drawing (after layout), so a plain holder avoids recomposition.
    val baseline = remember { FloatArray(1) }
    Text(
        text = text,
        style = textStyle,
        onTextLayout = { layout -> baseline[0] = layout.firstBaseline },
        fontWeight = FontWeight.Normal,
        color = textColor,
        // The flash pill is drawn outset around the text instead of padding it, so the
        // text's right edge stays aligned with the price line below.
        modifier = modifier.semantics {
            // Exposes the highlight state to UI tests; has no visual effect.
            percentHighlighted = highlighted
        }.drawBehind {
            if (background.alpha == 0f) return@drawBehind
            val px = PillHorizontalPadding.toPx()
            val py = PillVerticalPadding.toPx()
            // The pill hugs the glyphs (digits, + - . % never descend below the baseline)
            // rather than the layout box, which includes extra line-height padding.
            val top = baseline[0] - textStyle.fontSize.toPx() * DIGIT_HEIGHT_EM - py
            val bottom = baseline[0] + py
            drawRoundRect(
                color = background,
                topLeft = Offset(-px, top),
                size = Size(size.width + 2 * px, bottom - top),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        },
    )
}
