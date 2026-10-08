package com.grch.tradernettestasignment.feature.quotes.domain.repository

import com.grch.tradernettestasignment.feature.quotes.domain.model.QuoteStreamEvent
import kotlinx.coroutines.flow.Flow

/** Delivers partial quote updates and connection lifecycle events for the requested active ticker set. */
interface QuoteRepository {
    fun observeQuotes(tickers: List<String>): Flow<QuoteStreamEvent>
}
