package com.grch.tradernettestasignment.feature.quotes.impl

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.grch.tradernettestasignment.feature.quotes.api.domain.FixedTickers

/**
 * Immutable screen state owned by [QuotesViewModel].
 *
 * [rows] is seeded from [FixedTickers] in the spec's exact order and always
 * keeps every fixed row. When the top-securities API returns tickers not already
 * in the fixed list, they are appended (in API order) after the fixed rows; on
 * failure, an empty response, or no tickers beyond the fixed list, [rows] stays
 * exactly the fixed list. Arriving quote data never reorders, filters, or extends
 * [rows] further — only the screen-open source request can add rows.
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
    /**
     * What the screen should render. [rows] (tickers and symbols are known
     * upfront, so [BoardStatus.CONTENT] renders immediately — each row shimmers
     * only the specific fields it doesn't have yet) or the full-screen error
     * placeholder in [BoardStatus.ERROR].
     */
    val boardStatus: BoardStatus = BoardStatus.CONTENT,
)

/** See [QuotesViewModel]'s class doc for the exact state machine. */
enum class BoardStatus {
    CONTENT,
    ERROR,
}

/**
 * Display model for a single watchlist row.
 *
 * Fields are nullable until a quote arrives — a row with a `null` [lastPrice]
 * has never received any quote frame yet and renders with its price-area
 * fields shimmering rather than a dash. [flash] is transient and cleared by
 * the ViewModel after the flash window elapses. [minStep] is the security's
 * minimum price increment; per the `min-step-price-formatting` spec it drives
 * [lastPrice]/[change] formatting once known, and is `null` until the first
 * quote frame for that ticker arrives (it is never sent on later partial
 * updates, so it is merged like any other sparse field).
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
    val minStep: Double? = null,
    val flash: FlashDirection = FlashDirection.NONE,
)

/** Direction of the last price move, used to drive the row flash. */
enum class FlashDirection {
    NONE,
    UP,
    DOWN,
}
