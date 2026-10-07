package com.grch.tradernettestasignment.feature.quotes.impl

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.grch.tradernettestasignment.feature.quotes.api.domain.FixedTickers

/**
 * Immutable screen state owned by [QuotesViewModel].
 *
 * [rows] is seeded from [FixedTickers] in the spec's exact order and is shown
 * while the top-securities request is in flight on screen open. When the API
 * returns a non-empty set, [rows] is replaced by the API tickers in API order; on
 * failure or empty the fixed list stays (or becomes) the active set. Arriving
 * data never reorders, filters, or extends [rows].
 *
 * [source] is the currently active source, resolved by the API-on-screen-open
 * policy. It defaults to [QuoteSource.FIXED] and is never chosen by a UI control
 * or persisted preference.
 */
@Stable
data class QuotesState(
    val rows: List<QuoteRow> = FixedTickers.ALL.map { QuoteRow(ticker = it) },
    val isStreaming: Boolean = false,
    /** The active source chosen by the API-on-screen-open policy. */
    val source: QuoteSource = QuoteSource.FIXED,
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
