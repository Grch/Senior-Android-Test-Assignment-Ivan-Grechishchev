package com.grch.tradernettestasignment.feature.quotes.data.repository

import com.grch.tradernettestasignment.feature.quotes.data.parser.ParsedTopSecurities
import com.grch.tradernettestasignment.feature.quotes.data.parser.TopSecuritiesParseException
import com.grch.tradernettestasignment.feature.quotes.data.parser.TopSecuritiesResponseParser
import com.grch.tradernettestasignment.feature.quotes.data.parser.topSecuritiesRequestQuery
import com.grch.tradernettestasignment.feature.quotes.data.remote.TopSecuritiesService
import android.util.Log
import com.grch.tradernettestasignment.feature.quotes.domain.repository.TopSecuritiesDataSource
import com.grch.tradernettestasignment.feature.quotes.domain.model.TopSecuritiesError
import com.grch.tradernettestasignment.feature.quotes.domain.model.TopSecuritiesResult
import java.io.IOException
import java.util.concurrent.CancellationException
import javax.inject.Inject

/**
 * Retrofit-backed [TopSecuritiesDataSource] using Hilt-provided dependencies.
 *
 * Failure classification:
 * - [TopSecuritiesError.TRANSPORT] — an [IOException] (DNS, connection, timeout).
 * - [TopSecuritiesError.HTTP] — a non-success HTTP status.
 * - [TopSecuritiesError.API] — the documented `code`/`error`/`errMsg` payload.
 * - [TopSecuritiesError.PARSE] — an unreadable or malformed body.
 * - [TopSecuritiesError.EMPTY] — a valid response with no tickers.
 *
 * Expected failures are returned, never thrown; cancellation is rethrown.
 */
class TradernetTopSecuritiesDataSource @Inject constructor(
    private val service: TopSecuritiesService,
    private val parser: TopSecuritiesResponseParser,
) : TopSecuritiesDataSource {

    override suspend fun fetchTopSecurities(): TopSecuritiesResult {
        return try {
            val response = service.getTopSecurities(topSecuritiesRequestQuery())
            if (!response.isSuccessful) {
                Log.d(TAG, "http failure: ${response.code()}")
                response.errorBody()?.close()
                return TopSecuritiesResult.Failure(TopSecuritiesError.HTTP)
            }
            val raw = response.body()?.use { body -> body.string() }
            if (raw.isNullOrBlank()) {
                Log.d(TAG, "parse failure: empty body")
                return TopSecuritiesResult.Failure(TopSecuritiesError.PARSE)
            }
            when (val parsed = parser.parse(raw)) {
                is ParsedTopSecurities.DeclaredFailure -> {
                    Log.d(TAG, "api-declared failure")
                    TopSecuritiesResult.Failure(TopSecuritiesError.API)
                }

                is ParsedTopSecurities.Success -> {
                    if (parsed.securities.isEmpty()) {
                        Log.d(TAG, "empty tickers")
                        TopSecuritiesResult.Failure(TopSecuritiesError.EMPTY)
                    } else {
                        TopSecuritiesResult.Success(parsed.securities)
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (io: IOException) {
            Log.d(TAG, "transport failure: ${io.message}")
            TopSecuritiesResult.Failure(TopSecuritiesError.TRANSPORT)
        } catch (json: TopSecuritiesParseException) {
            Log.d(TAG, "parse failure: ${json.message}")
            TopSecuritiesResult.Failure(TopSecuritiesError.PARSE)
        } catch (exception: Exception) {
            Log.d(TAG, "parse failure: ${exception.message}")
            TopSecuritiesResult.Failure(TopSecuritiesError.PARSE)
        }
    }

    private companion object {
        const val TAG = "TopSecurities"
    }
}
