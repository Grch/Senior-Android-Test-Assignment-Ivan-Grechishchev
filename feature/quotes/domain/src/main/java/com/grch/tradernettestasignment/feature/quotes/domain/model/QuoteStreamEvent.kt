package com.grch.tradernettestasignment.feature.quotes.domain.model

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

    /** The connection ended or a connection attempt failed; a retry follows automatically unless [ConnectionLost] is emitted next. */
    data object Disconnected : QuoteStreamEvent

    /**
     * Terminal: repeated consecutive connection attempts failed and automatic
     * retries stopped. The stream completes after this event; a consumer must
     * resubscribe (e.g. on a manual "Try again") to connect again.
     */
    data object ConnectionLost : QuoteStreamEvent
}
