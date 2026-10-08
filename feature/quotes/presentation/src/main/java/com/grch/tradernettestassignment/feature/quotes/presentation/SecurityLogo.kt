package com.grch.tradernettestassignment.feature.quotes.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

private val LogoSize = 20.dp

/**
 * A security's logo on a neutral monogram tile. The monogram shows only while
 * no real logo is displayed: during loading, on a load failure, and on
 * Tradernet's own "no logo" response (a 1x1 fully transparent GIF, not an HTTP
 * error — see [QuoteRow.logoUrl]), detected by its 1x1 intrinsic size
 * since it otherwise reports as a successful load. Once a real logo loads, the
 * monogram is removed rather than left underneath, since most brand logos have
 * a transparent background and would otherwise show both at once.
 */
@Composable
internal fun SecurityLogo(ticker: String, logoUrl: String, modifier: Modifier = Modifier) {
    var showPlaceholder by remember(ticker, logoUrl) { mutableStateOf(true) }
    Box(
        modifier = modifier
            .size(LogoSize)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (showPlaceholder) {
            Text(
                text = ticker.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(logoUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.size(LogoSize),
            contentScale = ContentScale.Fit,
            onState = { state ->
                showPlaceholder = when (state) {
                    is AsyncImagePainter.State.Success -> {
                        val size = state.painter.intrinsicSize
                        size.width <= 1f || size.height <= 1f
                    }
                    else -> true
                }
            },
        )
    }
}
