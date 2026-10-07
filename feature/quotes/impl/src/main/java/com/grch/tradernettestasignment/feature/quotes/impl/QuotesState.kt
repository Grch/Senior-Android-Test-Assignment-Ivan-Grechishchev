package com.grch.tradernettestasignment.feature.quotes.impl

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.grch.tradernettestasignment.feature.quotes.impl.domain.FixedTickers

/**
 * Immutable screen state owned by [QuotesViewModel].
 *
 * [rows] is seeded from [FixedTickers] in the spec's exact order and is never
 * reordered, filtered, or extended by incoming data.
 */
@Stable
data class QuotesState(
    val rows: List<QuoteRow> = FixedTickers.ALL.map { QuoteRow(ticker = it) },
    val isStreaming: Boolean = false,
)

/**
 * Display model for a single watchlist row.
 *
 * Fields are nullable until a quote arrives. [flash] is transient and cleared by
 * the ViewModel after the flash window elapses.
 */
@Immutable
data class QuoteRow(
    val ticker: String,
    val exchange: String? = null,
    val name: String? = null,
    val lastPrice: Double? = null,
    val change: Double? = null,
    val percentChange: Double? = null,
    val previousClose: Double? = null,
    val flash: FlashDirection = FlashDirection.NONE,
)

/** Direction of the last price move, used to drive the row flash. */
enum class FlashDirection {
    NONE,
    UP,
    DOWN,
}
