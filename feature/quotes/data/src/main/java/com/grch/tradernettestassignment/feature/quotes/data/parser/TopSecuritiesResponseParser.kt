package com.grch.tradernettestassignment.feature.quotes.data.parser

import com.grch.tradernettestassignment.feature.quotes.data.model.TopSecuritiesResponseDto
import com.grch.tradernettestassignment.feature.quotes.domain.model.TopSecurityRef
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import java.io.IOException
import javax.inject.Inject
import org.json.JSONObject

private const val TOP_SECURITIES_LIMIT = 30

/**
 * Serializes the request for the `q` query parameter, e.g.
 * `{"cmd":"getTopSecurities","params":{"type":"stocks","exchange":"russia","gainers":0,"limit":30}}`.
 */
internal fun topSecuritiesRequestQuery(): String = JSONObject().apply {
    put("cmd", "getTopSecurities")
    put(
        "params",
        JSONObject().apply {
            put("type", "stocks")
            put("exchange", "russia")
            put("gainers", 0)
            put("limit", TOP_SECURITIES_LIMIT)
        },
    )
}.toString()

/** Parse result: [Success] in response order, or [DeclaredFailure] for the `code`/`error`/`errMsg` payload. */
internal sealed interface ParsedTopSecurities {
    data class Success(val securities: List<TopSecurityRef>) : ParsedTopSecurities
    data object DeclaredFailure : ParsedTopSecurities
}

/**
 * Parses `{"tickers":["GAZP",...],"details":{"GAZP":{"name":"..."}}}`; `details[<ticker>].name` is optional.
 * Throws [TopSecuritiesParseException] on malformed JSON or a missing `tickers` array.
 */
internal class TopSecuritiesResponseParser @Inject constructor(
    private val adapter: JsonAdapter<TopSecuritiesResponseDto>,
) {
    // Each malformed shape throws so the data source can classify it as a parse failure.
    @Suppress("ThrowsCount")
    fun parse(raw: String): ParsedTopSecurities {
        val root = try {
            adapter.fromJson(raw)
        } catch (exception: IOException) {
            throw TopSecuritiesParseException(exception)
        } catch (exception: JsonDataException) {
            throw TopSecuritiesParseException(exception)
        } ?: throw TopSecuritiesParseException()
        if (root.code != null || root.error != null || root.errorMessage != null) {
            return ParsedTopSecurities.DeclaredFailure
        }
        val tickerArray = root.tickers ?: throw TopSecuritiesParseException()
        val securities = buildList {
            for (value in tickerArray) {
                val ticker = (value as? String)?.takeIf { it.isNotBlank() } ?: continue
                val name = (root.details?.get(ticker)?.name as? String)
                    ?.takeIf { it.isNotBlank() }
                add(TopSecurityRef(ticker = ticker, name = name))
            }
        }
        if (tickerArray.isNotEmpty() && securities.isEmpty()) {
            // A populated array made entirely of invalid entries is a malformed
            // response, not a valid empty one: fail so the caller falls back.
            throw TopSecuritiesParseException()
        }
        return ParsedTopSecurities.Success(securities)
    }
}

internal class TopSecuritiesParseException(cause: Throwable? = null) :
    IllegalArgumentException("Malformed top-securities response", cause)
