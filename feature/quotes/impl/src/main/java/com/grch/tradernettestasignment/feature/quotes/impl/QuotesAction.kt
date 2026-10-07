package com.grch.tradernettestasignment.feature.quotes.impl

/** User-triggered interactions handled by [QuotesViewModel]. */
sealed interface QuotesAction {
    data object OnPlaceholderAction : QuotesAction
}
