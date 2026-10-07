package com.grch.tradernettestasignment.feature.quotes.impl.data

import com.grch.tradernettestasignment.feature.quotes.api.domain.Quote
import com.grch.tradernettestasignment.feature.quotes.api.domain.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

/** Decodes raw Tradernet quote frames and maps valid partial updates to domain values. */
class TradernetQuoteRepository @Inject constructor(
    private val remoteDataSource: WebSocketRemoteDataSource,
    private val parser: QuoteMessageParser,
) : QuoteRepository {
    override fun observeQuotes(tickers: List<String>): Flow<Quote> =
        remoteDataSource.observeFrames(tickers).mapNotNull(parser::parse)
}
