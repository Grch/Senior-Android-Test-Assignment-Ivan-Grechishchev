package com.grch.tradernettestasignment.feature.quotes.impl.data

import com.grch.tradernettestasignment.feature.quotes.impl.domain.Quote
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Wire DTO for the object delivered under the Tradernet `q` event.
 *
 * The documented field layout
 * (`https://tradernet.com/tradernet-api/quotes-get-changes/?site_lang=en`) uses
 * named fields, not array positions:
 * - `c`   ticker
 * - `ltr` exchange of the latest trade
 * - `name` security name
 * - `ltp` last-trade price
 * - `chg` signed point change relative to the previous close
 * - `pcp` percent change relative to the previous close
 * - `pp`  previous close
 *
 * All fields except [ticker] are optional because later frames are partial.
 */
internal data class QuoteMessageDto(
    val ticker: String,
    val exchange: String?,
    val name: String?,
    val lastPrice: Double?,
    val change: Double?,
    val percentChange: Double?,
    val previousClose: Double?,
)

/** Maps the parsed wire DTO onto the domain model. */
internal fun QuoteMessageDto.toQuote(): Quote = Quote(
    ticker = ticker,
    exchange = exchange,
    name = name,
    lastPrice = lastPrice,
    change = change,
    percentChange = percentChange,
    previousClose = previousClose,
)

/**
 * Builds the one-and-only subscription text frame for the `quotes` event.
 *
 * The official page's browser helper shows an invalid two-argument
 * `ws.send("quotes", ...)` call (not a WebSocket API). The value that must be
 * sent is the serialized array `["quotes", ["<ticker>", ...]]` as a single text
 * frame, which is what this produces.
 */
internal fun subscriptionFrame(tickers: List<String>): String {
    val tickerArray = JSONArray().apply { tickers.forEach(::put) }
    return JSONArray().apply {
        put("quotes")
        put(tickerArray)
    }.toString()
}

/**
 * Parses a raw Tradernet socket message into a [Quote], or returns `null` when
 * the message is not a `q` quote payload.
 *
 * Observed envelope: `["q", { ...named fields... }, "wstm=..."]`. The quote
 * payload at index 1 is a single JSON object. The official page's browser helper
 * calls `data.forEach` and is annotated `QuoteInfoAnswer[]`, which implies an
 * array; a live capture confirms the object form, so only the object form is
 * supported here.
 */
internal fun parseQuoteMessage(raw: String): Quote? {
    val frame = try {
        JSONArray(raw)
    } catch (e: JSONException) {
        return null
    }
    if (frame.optString(0) != QUOTES_EVENT) return null
    val payload = frame.optJSONObject(1) ?: return null
    val ticker = payload.optString(QUOTE_TICKER).takeIf { it.isNotEmpty() } ?: return null

    val dto = QuoteMessageDto(
        ticker = ticker,
        exchange = payload.stringOrNull("ltr"),
        name = payload.stringOrNull("name"),
        lastPrice = payload.doubleOrNull("ltp"),
        change = payload.doubleOrNull("chg"),
        percentChange = payload.doubleOrNull("pcp"),
        previousClose = payload.doubleOrNull("pp"),
    )
    return dto.toQuote()
}

private const val QUOTES_EVENT = "q"
private const val QUOTE_TICKER = "c"

private fun JSONObject.stringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotEmpty() }
}

private fun JSONObject.doubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return when (val value = opt(key)) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}
