package com.grch.tradernettestassignment.feature.quotes.data.parser

import com.grch.tradernettestassignment.feature.quotes.data.model.QuoteFrameDto
import com.grch.tradernettestassignment.feature.quotes.data.model.QuotePayloadDto
import com.grch.tradernettestassignment.feature.quotes.domain.model.Quote
import com.squareup.moshi.Moshi
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class QuoteMessageParserTest {
    private val moshi = Moshi.Builder().build()
    private val frameAdapter = QuoteFrameJsonAdapter(moshi.adapter(QuotePayloadDto::class.java))
    private val parser = QuoteMessageParser(frameAdapter)

    @Test
    fun fullQuoteFrameMapsAllFields() {
        val raw = """["q",{"c":"SBER","ltr":"MCX","name":"Sberbank","ltp":312.45,"chg":-1.5,""" +
            """"pcp":-0.48,"pp":313.95,"min_step":0.01,"unknown":"x"},"wstm=123"]"""

        assertEquals(
            Quote("SBER", "MCX", "Sberbank", 312.45, -1.5, -0.48, 313.95, 0.01),
            parser.parse(raw),
        )
    }

    @Test
    fun partialUpdateLeavesMissingFieldsNull() {
        val quote = parser.parse("""["q",{"c":"GAZP","ltp":150.0,"chg":2}]""")

        assertEquals(Quote(ticker = "GAZP", lastPrice = 150.0, change = 2.0), quote)
    }

    @Test
    fun explicitNullFieldsAreNull() {
        val quote = parser.parse("""["q",{"c":"GAZP","ltp":null,"name":null}]""")

        assertEquals(Quote(ticker = "GAZP"), quote)
    }

    @Test
    fun nonQuoteEventIsIgnored() {
        assertNull(parser.parse("""["userStats",{"c":"SBER","ltp":1.0}]"""))
        assertNull(parser.parse("""["markets",{"c":"SBER"}]"""))
    }

    @Test
    fun missingOrBlankTickerIsIgnored() {
        assertNull(parser.parse("""["q",{"ltp":1.0}]"""))
        assertNull(parser.parse("""["q",{"c":"  ","ltp":1.0}]"""))
        assertNull(parser.parse("""["q",{"c":null}]"""))
    }

    @Test
    fun arrayPayloadIsNotSupported() {
        assertNull(parser.parse("""["q",[{"c":"SBER","ltp":1.0}]]"""))
    }

    @Test
    fun malformedInputReturnsNullWithoutThrowing() {
        listOf(
            "",
            "not json",
            "{",
            "[",
            "[\"q\",{\"c\":\"SBER\",",
            "{\"c\":\"SBER\"}",
            "null",
            "[]",
            "[1,2,3]",
            """["q",{"c":"SBER","ltp":"abc"}]""",
        ).forEach { assertNull("input: $it", parser.parse(it)) }
    }

    @Test
    fun frameAdapterReadsEventAndPayload() {
        val frame = frameAdapter.fromJson("""["q",{"c":"SBER","ltp":1.5},"meta"]""")

        assertEquals(QuoteFrameDto("q", QuotePayloadDto(ticker = "SBER", lastPrice = 1.5)), frame)
    }

    @Test
    fun frameAdapterSkipsPayloadOfOtherEvents() {
        assertEquals(QuoteFrameDto("x", null), frameAdapter.fromJson("""["x",{"c":"SBER"}]"""))
    }

    @Test
    fun frameAdapterHandlesNonStringEventAndNull() {
        assertEquals(QuoteFrameDto(null, null), frameAdapter.fromJson("""[1,{"c":"SBER"}]"""))
        assertNull(frameAdapter.fromJson("null"))
    }

    @Test
    fun frameAdapterIsInboundOnly() {
        assertThrows(UnsupportedOperationException::class.java) {
            // The assertion is that serializing throws; there is no result to check.
            @Suppress("IgnoredReturnValue")
            frameAdapter.toJson(QuoteFrameDto("q", null))
        }
    }

    @Test
    fun payloadToQuoteRequiresNonBlankTicker() {
        assertNull(QuotePayloadDto().toQuote())
        assertNull(QuotePayloadDto(ticker = "").toQuote())
        assertEquals(Quote("A", minStep = 0.5), QuotePayloadDto(ticker = "A", minStep = 0.5).toQuote())
    }

    @Test
    fun subscriptionFrameIsQuotesEventWithTickerArray() {
        val array = JSONArray(subscriptionFrame(listOf("SBER", "GAZP.US")))

        assertEquals(2, array.length())
        assertEquals("quotes", array.getString(0))
        val tickers = array.getJSONArray(1)
        assertEquals(listOf("SBER", "GAZP.US"), (0 until tickers.length()).map { tickers.getString(it) })
    }

    @Test
    fun subscriptionFrameWithNoTickersHasEmptyArray() {
        val array = JSONArray(subscriptionFrame(emptyList()))

        assertEquals("quotes", array.getString(0))
        assertEquals(0, array.getJSONArray(1).length())
    }

    @Test
    fun subscriptionFrameIsValidJsonArray() {
        val array = JSONArray(subscriptionFrame(listOf("A", "B")))

        assertEquals("quotes", array.getString(0))
        assertNotNull(array.getJSONArray(1))
        assertEquals(2, array.getJSONArray(1).length())
    }
}
