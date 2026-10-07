package com.grch.tradernettestasignment.feature.quotes.impl.data

import com.grch.tradernettestasignment.feature.quotes.impl.domain.TopSecurityRef
import org.json.JSONException
import org.json.JSONObject

/**
 * Serializes the verified top-securities request object for the `q` query
 * parameter:
 *
 * `{"cmd":"getTopSecurities","params":{"type":"stocks","exchange":"russia","gainers":0,"limit":30}}`
 *
 * The values are the live-verified contract recorded in design.md; no auth is
 * involved.
 */
internal fun topSecuritiesRequestQuery(): String = JSONObject().apply {
    put("cmd", "getTopSecurities")
    put(
        "params",
        JSONObject().apply {
            put("type", "stocks")
            put("exchange", "russia")
            put("gainers", 0)
            put("limit", 30)
        },
    )
}.toString()

/**
 * Result of parsing a raw top-securities response body.
 *
 * [Success] carries the securities in response order. [DeclaredFailure] marks the
 * documented `code`/`error`/`errMsg` failure payload. Malformed JSON or a missing
 * `tickers` array throws [JSONException] so the data source can classify it as a
 * parse failure.
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
internal fun parseTopSecuritiesResponse(raw: String): ParsedTopSecurities {
    val root = JSONObject(raw)
    if (root.has("code") || root.has("error") || root.has("errMsg")) {
        return ParsedTopSecurities.DeclaredFailure
    }
    if (!root.has("tickers")) {
        throw JSONException("Response has no tickers array")
    }
    val tickerArray = root.optJSONArray("tickers")
        ?: throw JSONException("tickers is not an array")
    val details = root.optJSONObject("details")
    val securities = buildList {
        for (index in 0 until tickerArray.length()) {
            // Only accept a genuine non-blank JSON string. JSON null is exposed
            // as JSONObject.NULL (not a String), so it is skipped rather than
            // becoming the literal ticker "null"; numbers/objects are skipped too.
            val ticker = (tickerArray.opt(index) as? String)
                ?.takeIf { it.isNotBlank() }
                ?: continue
            val name = (details?.optJSONObject(ticker)?.opt("name") as? String)
                ?.takeIf { it.isNotBlank() }
            add(TopSecurityRef(ticker = ticker, name = name))
        }
    }
    if (tickerArray.length() > 0 && securities.isEmpty()) {
        // A populated array made entirely of invalid entries is a malformed
        // response, not a valid empty one: fail so the caller falls back.
        throw JSONException("tickers contains no valid string entries")
    }
    return ParsedTopSecurities.Success(securities)
}
