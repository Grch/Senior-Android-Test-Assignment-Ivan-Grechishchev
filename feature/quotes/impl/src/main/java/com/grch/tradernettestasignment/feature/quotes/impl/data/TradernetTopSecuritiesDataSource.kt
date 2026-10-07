package com.grch.tradernettestasignment.feature.quotes.impl.data

import android.util.Log
import com.grch.tradernettestasignment.feature.quotes.impl.domain.TopSecuritiesError
import com.grch.tradernettestasignment.feature.quotes.impl.domain.TopSecuritiesResult
import java.io.IOException
import java.util.concurrent.CancellationException
import org.json.JSONException

/**
 * Fetches the ordered top-securities ticker IDs from the verified Tradernet REST
 * endpoint.
 *
 * A single call is one attempt; fallback and retry policy belong to the caller.
 */
interface TopSecuritiesDataSource {
    /**
     * Returns [TopSecuritiesResult.Success] only for a non-empty parsed `tickers`
     * list; every failure is a [TopSecuritiesResult.Failure] with a distinct
     * [TopSecuritiesError].
     */
    suspend fun fetchTopSecurities(): TopSecuritiesResult
}

/**
 * Retrofit-backed [TopSecuritiesDataSource] using the process-wide
 * [TradernetClientFactory] instance.
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
class TradernetTopSecuritiesDataSource(
    private val service: TopSecuritiesService = TradernetClientFactory.topSecuritiesService,
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
            when (val parsed = parseTopSecuritiesResponse(raw)) {
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
        } catch (json: JSONException) {
            Log.d(TAG, "parse failure: ${json.message}")
            TopSecuritiesResult.Failure(TopSecuritiesError.PARSE)
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            Log.d(TAG, "parse failure: ${exception.message}")
            TopSecuritiesResult.Failure(TopSecuritiesError.PARSE)
        }
    }

    private companion object {
        const val TAG = "TopSecurities"
    }
}
