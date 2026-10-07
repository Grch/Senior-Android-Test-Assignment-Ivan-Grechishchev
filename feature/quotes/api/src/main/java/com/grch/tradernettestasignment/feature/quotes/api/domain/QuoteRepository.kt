package com.grch.tradernettestasignment.feature.quotes.api.domain

import kotlinx.coroutines.flow.Flow

/** Delivers partial quote updates for the requested active ticker set. */
interface QuoteRepository {
    fun observeQuotes(tickers: List<String>): Flow<Quote>
}
