package com.grch.tradernettestasignment.feature.quotes.api.domain

import kotlinx.coroutines.flow.Flow

/** Delivers partial quote updates and connection lifecycle events for the requested active ticker set. */
interface QuoteRepository {
    fun observeQuotes(tickers: List<String>): Flow<QuoteStreamEvent>
}

/**
 * An event from the live quote stream: either a partial [Quote] update, or a
 * connection lifecycle signal a consumer can use to tell a genuine failure
 * (no network, socket rejected, ...) apart from data simply not having
 * arrived yet.
 */
sealed interface QuoteStreamEvent {
    data class Update(val quote: Quote) : QuoteStreamEvent

    /** The socket is currently connected; frames may follow at any time. */
    data object Connected : QuoteStreamEvent

    /** The connection ended or a connection attempt failed; a retry follows automatically. */
    data object Disconnected : QuoteStreamEvent
}
