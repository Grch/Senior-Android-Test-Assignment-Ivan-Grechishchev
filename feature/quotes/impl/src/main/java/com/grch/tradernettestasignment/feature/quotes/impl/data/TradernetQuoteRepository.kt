package com.grch.tradernettestasignment.feature.quotes.impl.data

import com.grch.tradernettestasignment.feature.quotes.api.domain.QuoteRepository
import com.grch.tradernettestasignment.feature.quotes.api.domain.QuoteStreamEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

/** Decodes raw Tradernet quote frames and maps valid partial updates and connection events to domain values. */
class TradernetQuoteRepository @Inject constructor(
    private val remoteDataSource: WebSocketRemoteDataSource,
    private val parser: QuoteMessageParser,
) : QuoteRepository {
    override fun observeQuotes(tickers: List<String>): Flow<QuoteStreamEvent> =
        remoteDataSource.observeFrames(tickers).mapNotNull { event ->
            when (event) {
                is SocketEvent.Frame -> parser.parse(event.text)?.let(QuoteStreamEvent::Update)
                SocketEvent.Opened -> QuoteStreamEvent.Connected
                SocketEvent.Closed -> QuoteStreamEvent.Disconnected
            }
        }
}
