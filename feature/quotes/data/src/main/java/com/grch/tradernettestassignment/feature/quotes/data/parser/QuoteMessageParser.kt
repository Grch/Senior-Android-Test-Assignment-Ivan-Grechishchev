package com.grch.tradernettestassignment.feature.quotes.data.parser

import com.grch.tradernettestassignment.feature.quotes.data.model.QuoteFrameDto
import com.grch.tradernettestassignment.feature.quotes.data.model.QuotePayloadDto
import com.grch.tradernettestassignment.feature.quotes.domain.model.Quote
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import javax.inject.Inject
import org.json.JSONArray

/** Maps the parsed wire DTO onto the domain model. */
internal fun QuotePayloadDto.toQuote(): Quote? {
    val validTicker = ticker?.takeIf(String::isNotBlank) ?: return null
    return Quote(
        ticker = validTicker,
        exchange = exchange,
        name = name,
        lastPrice = lastPrice,
        change = change,
        percentChange = percentChange,
        previousClose = previousClose,
        minStep = minStep,
    )
}

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
internal class QuoteMessageParser @Inject constructor(
    private val quoteFrameAdapter: JsonAdapter<QuoteFrameDto>,
) {
    fun parse(raw: String): Quote? = try {
        quoteFrameAdapter.fromJson(raw)
            ?.takeIf { it.event == QUOTES_EVENT }
            ?.payload
            ?.toQuote()
    } catch (_: Exception) {
        // Unrelated and malformed frames are not quote updates.
        null
    }
}

private const val QUOTES_EVENT = "q"

/** Moshi adapter for the documented/live-verified `event, payload, metadata` tuple. */
internal class QuoteFrameJsonAdapter(
    private val payloadAdapter: JsonAdapter<QuotePayloadDto>,
) : JsonAdapter<QuoteFrameDto>() {
    override fun fromJson(reader: JsonReader): QuoteFrameDto? {
        if (reader.peek() == JsonReader.Token.NULL) return reader.nextNull()
        reader.beginArray()
        val event = if (reader.hasNext() && reader.peek() == JsonReader.Token.STRING) {
            reader.nextString()
        } else {
            if (reader.hasNext()) reader.skipValue()
            null
        }
        val payload = if (event == QUOTES_EVENT && reader.hasNext() &&
            reader.peek() == JsonReader.Token.BEGIN_OBJECT
        ) {
            payloadAdapter.fromJson(reader)
        } else {
            if (reader.hasNext()) reader.skipValue()
            null
        }
        while (reader.hasNext()) reader.skipValue()
        reader.endArray()
        return QuoteFrameDto(event = event, payload = payload)
    }

    override fun toJson(writer: JsonWriter, value: QuoteFrameDto?) {
        throw UnsupportedOperationException("Quote frames are inbound-only")
    }
}
