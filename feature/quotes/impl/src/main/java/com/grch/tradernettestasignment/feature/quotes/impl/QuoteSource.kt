package com.grch.tradernettestasignment.feature.quotes.impl

/**
 * The source that currently supplies the board's active ticker set.
 *
 * This is a read-only state indicator, not a user-selectable input: the agreed
 * behavior requests the top-securities API by default whenever the quotes screen
 * opens. [FIXED] means the exact `live-quotes-board` fixed watchlist is active —
 * it is both the initial set shown while the request is in flight and the
 * fallback when the API fails or returns an empty list. [TOP_SECURITIES] means a
 * non-empty API response replaced the fixed list.
 *
 * There is no UI toggle, feature flag, build switch, or persisted preference for
 * choosing between these; the API-on-screen-open policy decides the active
 * source.
 */
enum class QuoteSource {
    /** The fixed `live-quotes-board` watchlist is the active ticker set. */
    FIXED,

    /** A non-empty top-securities API response is the active ticker set. */
    TOP_SECURITIES,
}
