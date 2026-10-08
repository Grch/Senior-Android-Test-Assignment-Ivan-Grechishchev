package com.grch.tradernettestassignment.feature.quotes.data.parser

import com.grch.tradernettestassignment.feature.quotes.data.model.TopSecuritiesResponseDto
import com.grch.tradernettestassignment.feature.quotes.domain.model.TopSecurityRef
import com.squareup.moshi.Moshi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TopSecuritiesResponseParserTest {
    private val parser = TopSecuritiesResponseParser(
        Moshi.Builder().build().adapter(TopSecuritiesResponseDto::class.java),
    )

    @Test
    fun successKeepsOrderAndAttachesNames() {
        val raw = """{"tickers":["GAZP","SBER","LKOH"],"details":{"GAZP":{"name":"Gazprom"},""" +
            """"SBER":{"name":"Sberbank"}},"buttons":{"x":1}}"""

        assertEquals(
            ParsedTopSecurities.Success(
                listOf(
                    TopSecurityRef("GAZP", "Gazprom"),
                    TopSecurityRef("SBER", "Sberbank"),
                    TopSecurityRef("LKOH", null),
                ),
            ),
            parser.parse(raw),
        )
    }

    @Test
    fun blankNamesAndInvalidTickersAreDropped() {
        val raw = """{"tickers":["GAZP",null,5,"  ","SBER"],"details":{"GAZP":{"name":" "},"SBER":{"name":7}}}"""

        assertEquals(
            ParsedTopSecurities.Success(listOf(TopSecurityRef("GAZP"), TopSecurityRef("SBER"))),
            parser.parse(raw),
        )
    }

    @Test
    fun emptyTickersIsEmptySuccess() {
        assertEquals(ParsedTopSecurities.Success(emptyList()), parser.parse("""{"tickers":[]}"""))
    }

    @Test
    fun declaredFailurePayloads() {
        assertEquals(ParsedTopSecurities.DeclaredFailure, parser.parse("""{"code":2,"errMsg":"bad"}"""))
        assertEquals(ParsedTopSecurities.DeclaredFailure, parser.parse("""{"error":"x"}"""))
        assertEquals(
            ParsedTopSecurities.DeclaredFailure,
            parser.parse("""{"tickers":["A"],"errMsg":"bad"}"""),
        )
    }

    @Test
    fun malformedBodiesThrowParseException() {
        listOf(
            "not json",
            "{",
            "",
            "null",
            "[]",
            "{}",
            """{"tickers":"SBER"}""",
            """{"tickers":[null,1,""]}""",
        ).forEach { raw ->
            assertThrows("input: $raw", TopSecuritiesParseException::class.java) { parser.parse(raw) }
        }
    }

    @Test
    fun requestQueryHasVerifiedContract() {
        val json = JSONObject(topSecuritiesRequestQuery())
        val params = json.getJSONObject("params")

        assertEquals("getTopSecurities", json.getString("cmd"))
        assertEquals(2, json.length())
        assertEquals("stocks", params.getString("type"))
        assertEquals("russia", params.getString("exchange"))
        assertEquals(0, params.getInt("gainers"))
        assertEquals(30, params.getInt("limit"))
        assertEquals(4, params.length())
    }
}
