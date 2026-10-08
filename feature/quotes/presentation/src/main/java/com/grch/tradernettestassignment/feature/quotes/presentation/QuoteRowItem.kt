package com.grch.tradernettestassignment.feature.quotes.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val PLACEHOLDER = "—"
private const val PENDING_NAME_WIDTH_FRACTION = 0.55f

/**
 * A single watchlist row. While [row] has no [QuoteRow.lastPrice] yet (no quote
 * frame has arrived for it), the name/exchange line and the whole price area
 * shimmer using [shimmerBrush] instead of showing a dash — the ticker itself is
 * always known upfront, so only it and the logo render immediately either way.
 */
@Composable
internal fun QuoteRowItem(
    row: QuoteRow,
    shimmerBrush: Brush?,
    modifier: Modifier = Modifier,
) {
    val lastPrice = row.lastPrice
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuoteNameColumn(
                row = row,
                isPending = lastPrice == null,
                shimmerBrush = shimmerBrush,
                modifier = Modifier.weight(1f),
            )
            if (lastPrice == null) {
                PendingPriceColumn(shimmerBrush = shimmerBrush)
            } else {
                PriceColumn(row = row, lastPrice = lastPrice)
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** Left column: logo + ticker, and the exchange/name line (or its shimmer while pending). */
@Composable
private fun QuoteNameColumn(
    row: QuoteRow,
    isPending: Boolean,
    shimmerBrush: Brush?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SecurityLogo(ticker = row.ticker, logoUrl = row.logoUrl)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = row.ticker,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = primaryTextColor(),
            )
        }
        val secondary = listOfNotNull(row.exchange, row.name).joinToString(" | ")
        if (isPending) {
            ShimmerBlock(
                brush = shimmerBrush,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth(PENDING_NAME_WIDTH_FRACTION)
                    .height(12.dp),
            )
        } else if (secondary.isNotEmpty()) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PendingPriceColumn(shimmerBrush: Brush?) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ShimmerBlock(brush = shimmerBrush, modifier = Modifier.width(52.dp).height(20.dp))
        ShimmerBlock(brush = shimmerBrush, modifier = Modifier.width(72.dp).height(14.dp))
    }
}

/** Right column: percent badge above the last price and absolute change. */
@Composable
private fun PriceColumn(row: QuoteRow, lastPrice: Double) {
    Column(horizontalAlignment = Alignment.End) {
        PercentBadge(
            text = row.percentChange?.let(::formatPercent) ?: PLACEHOLDER,
            sign = row.percentChange,
            flash = row.flash,
            flashTick = row.flashTick,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatPrice(lastPrice, row.minStep),
                style = MaterialTheme.typography.bodySmall,
                color = primaryTextColor(),
            )
            row.change?.let { change ->
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${formatSignedPrice(change, row.minStep)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryTextColor(),
                )
            }
        }
    }
}
