package com.grch.tradernettestasignment.feature.quotes.impl

/**
 * Immutable screen state owned by [QuotesViewModel].
 *
 * Placeholder content only; the live quotes data is added by the
 * `live-quotes-board` change.
 */
data class QuotesState(
    val title: String = "Quotes",
    val placeholderMessage: String = "The live quotes board will be implemented in a later change.",
    val isActionDispatched: Boolean = false,
)
