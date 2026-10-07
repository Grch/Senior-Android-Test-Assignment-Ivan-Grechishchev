package com.grch.tradernettestasignment.feature.quotes.impl

/**
 * The source that currently supplies the board's active ticker set.
 *
 * This is a read-only state indicator, not a user-selectable input: the agreed
 * behavior requests the top-securities API by default whenever the quotes screen
 * opens, to augment — never replace — the required `live-quotes-board` fixed
 * watchlist. [FIXED] means only the fixed watchlist is active, either because the
 * request is still in flight, it failed, or it returned no ticker beyond the
 * fixed list. [AUGMENTED] means the fixed watchlist plus one or more extra
 * tickers appended from a non-empty top-securities response.
 *
 * There is no UI toggle, feature flag, build switch, or persisted preference for
 * choosing between these; the API-on-screen-open policy decides the active
 * source.
 */
enum class QuoteSource {
    /** Only the fixed `live-quotes-board` watchlist is the active ticker set. */
    FIXED,

    /** The fixed watchlist plus tickers appended from the top-securities API. */
    AUGMENTED,
}
