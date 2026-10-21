package com.grch.tradernettestassignment.feature.quotes.domain.model

/** An event from the live quote stream: a partial [Quote] update or a connection lifecycle signal. */
sealed interface QuoteStreamEvent {
    data class Update(val quote: Quote) : QuoteStreamEvent

    /** The socket is currently connected; frames may follow at any time. */
    data object Connected : QuoteStreamEvent

/** The connection ended or an attempt failed; a retry follows unless [ConnectionLost] is emitted next. */
    data object Disconnected : QuoteStreamEvent

/** Terminal: automatic retries stopped and the stream completes; resubscribe to connect again. */
    data object ConnectionLost : QuoteStreamEvent
}
