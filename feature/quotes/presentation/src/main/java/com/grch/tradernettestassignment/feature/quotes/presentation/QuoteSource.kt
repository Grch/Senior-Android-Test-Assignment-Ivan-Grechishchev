package com.grch.tradernettestassignment.feature.quotes.presentation

/** Which ticker set is active: [FIXED] is the watchlist only, [AUGMENTED] adds top-securities tickers. */
internal enum class QuoteSource {
    /** Only the fixed watchlist is the active ticker set. */
    FIXED,

    /** The fixed watchlist plus tickers appended from the top-securities API. */
    AUGMENTED,
}
