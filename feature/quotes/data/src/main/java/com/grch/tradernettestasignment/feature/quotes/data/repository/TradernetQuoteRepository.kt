package com.grch.tradernettestasignment.feature.quotes.data.repository

import com.grch.tradernettestasignment.feature.quotes.data.parser.QuoteMessageParser
import com.grch.tradernettestasignment.feature.quotes.data.remote.SocketEvent
import com.grch.tradernettestasignment.feature.quotes.data.remote.WebSocketRemoteDataSource
import com.grch.tradernettestasignment.feature.quotes.domain.repository.QuoteRepository
import com.grch.tradernettestasignment.feature.quotes.domain.model.QuoteStreamEvent
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
                SocketEvent.GaveUp -> QuoteStreamEvent.ConnectionLost
            }
        }
}
