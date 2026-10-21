package com.grch.tradernettestassignment.feature.quotes.presentation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Immutable screen state owned by [QuotesViewModel]. [rows] always holds the fixed list, optionally
 * followed by extra top-securities tickers; quote updates never add, remove or reorder rows.
 */
@Stable
internal data class QuotesState(
    val rows: List<QuoteRow> = emptyList(),
    val isStreaming: Boolean = false,
    /** The active source chosen by the API-on-screen-open policy. */
    val source: QuoteSource = QuoteSource.FIXED,
    /** [rows] while [BoardStatus.CONTENT]; an error placeholder once the connection is lost. */
    val boardStatus: BoardStatus = BoardStatus.CONTENT,
)

/** See [QuotesViewModel]'s class doc for the exact state machine. */
internal enum class BoardStatus {
    CONTENT,
    ERROR,

    /** Manual retry in progress: the board is dimmed behind a progress indicator. */
    RECONNECTING,
}

/**
 * Display model for a watchlist row. Fields stay null until a quote arrives; [flashTick] is bumped on
 * each directional change to trigger the highlight. [minStep] arrives only on the first frame.
 */
@Immutable
internal data class QuoteRow(
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
internal enum class FlashDirection {
    NONE,
    UP,
    DOWN,
}
