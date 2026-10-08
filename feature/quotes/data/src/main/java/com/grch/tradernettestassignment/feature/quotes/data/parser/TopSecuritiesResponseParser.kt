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
 * Serializes the top-securities request object for the `q` query parameter:
 *
 * `{"cmd":"getTopSecurities","params":{"type":"stocks","exchange":"russia","gainers":0,"limit":30}}`
 *
 * No auth is involved.
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

/**
 * Result of parsing a raw top-securities response body.
 *
 * [Success] carries the securities in response order. [DeclaredFailure] marks the
 * documented `code`/`error`/`errMsg` failure payload. Malformed JSON or a missing
 * `tickers` array throws [TopSecuritiesParseException] so the data source can
 * classify it as a parse failure.
 */
internal sealed interface ParsedTopSecurities {
    data class Success(val securities: List<TopSecurityRef>) : ParsedTopSecurities
    data object DeclaredFailure : ParsedTopSecurities
}

/**
 * Parses a raw response body.
 *
 * Success shape (documented and live-verified):
 * `{"tickers":["GAZP",...],"details":{"GAZP":{"name":"..."}}}`. The ordered
 * `tickers` array supplies the ticker IDs; `details[<ticker>].name` supplies
 * optional name metadata only. Any unrelated objects (for example `buttons` in
 * the live response) are ignored.
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
