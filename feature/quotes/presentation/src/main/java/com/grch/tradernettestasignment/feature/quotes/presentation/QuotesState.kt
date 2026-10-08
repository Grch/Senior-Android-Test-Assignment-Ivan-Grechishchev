package com.grch.tradernettestasignment.feature.quotes.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Immutable screen state owned by [QuotesViewModel].
 *
 * [rows] is seeded by [QuotesViewModel] from the fixed watchlist in the spec's exact order and always
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
    val rows: List<QuoteRow> = emptyList(),
    val isStreaming: Boolean = false,
    /** The active source chosen by the API-on-screen-open policy. */
    val source: QuoteSource = QuoteSource.FIXED,
    /**
     * What the screen should render. [rows] (tickers and symbols are known
     * upfront, so [BoardStatus.CONTENT] renders immediately — each row shimmers
     * only the specific fields it doesn't have yet) or the full-screen error
     * placeholder in [BoardStatus.ERROR], shown once the connection is lost
     * (repeated reconnect attempts failed). After a manual retry it is
     * [BoardStatus.RECONNECTING]: the board dimmed behind a progress indicator.
     */
    val boardStatus: BoardStatus = BoardStatus.CONTENT,
)

/** See [QuotesViewModel]'s class doc for the exact state machine. */
enum class BoardStatus {
    CONTENT,
    ERROR,

    /**
     * Manual retry in progress; the board is shown dimmed behind a progress
     * indicator until the socket connects or the retry gives up.
     */
    RECONNECTING,
}

/**
 * Display model for a single watchlist row.
 *
 * Fields are nullable until a quote arrives — a row with a `null` [lastPrice]
 * has never received any quote frame yet and renders with its price-area
 * fields shimmering rather than a dash. [flash] is the direction of the last
 * price change (never auto-cleared by the ViewModel); [flashTick] is a counter
 * bumped on each directional change, which the UI uses to trigger a short
 * highlight. [minStep] is the security's
 * minimum price increment; per the `min-step-price-formatting` spec it drives
 * [lastPrice]/[change] formatting once known, and is `null` until the first
 * quote frame for that ticker arrives (it is never sent on later partial
 * updates, so it is merged like any other sparse field).
 */
@Immutable
data class QuoteRow(
    val ticker: String,
    /** Resolved security-logo URL, supplied by the domain's logo URL provider. */
    val logoUrl: String,
    val exchange: String? = null,
    val name: String? = null,
    val lastPrice: Double? = null,
    val change: Double? = null,
    val percentChange: Double? = null,
    val previousClose: Double? = null,
    val minStep: Double? = null,
    val flash: FlashDirection = FlashDirection.NONE,
    val flashTick: Int = 0,
)

/** Direction of the last price move, used to color the percent-badge highlight. */
enum class FlashDirection {
    NONE,
    UP,
    DOWN,
}
