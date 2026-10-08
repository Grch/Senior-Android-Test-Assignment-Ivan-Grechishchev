package com.grch.tradernettestassignment.feature.quotes.presentation

/**
 * The source that currently supplies the board's active ticker set. A read-only
 * state indicator, not a user-selectable input.
 *
 * [FIXED] means only the fixed watchlist is active: the top-securities request is
 * still in flight, failed, or returned no ticker beyond the fixed list.
 * [AUGMENTED] means the fixed watchlist plus extra tickers appended from a
 * non-empty top-securities response.
 */
enum class QuoteSource {
    /** Only the fixed watchlist is the active ticker set. */
    FIXED,

    /** The fixed watchlist plus tickers appended from the top-securities API. */
    AUGMENTED,
}
