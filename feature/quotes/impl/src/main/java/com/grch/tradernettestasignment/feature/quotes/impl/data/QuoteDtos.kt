package com.grch.tradernettestasignment.feature.quotes.impl.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO for the named object at index 1 of the verified Tradernet `q` frame.
 * The documented fields (`c`, `ltr`, `name`, `ltp`, `chg`, `pcp`, `pp`) use
 * names rather than array positions, and all but the ticker remain nullable
 * because later quote frames may contain partial updates.
 */
@JsonClass(generateAdapter = false)
data class QuotePayloadDto(
    @param:Json(name = "c") val ticker: String? = null,
    @param:Json(name = "ltr") val exchange: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "ltp") val lastPrice: Double? = null,
    @param:Json(name = "chg") val change: Double? = null,
    @param:Json(name = "pcp") val percentChange: Double? = null,
    @param:Json(name = "pp") val previousClose: Double? = null,
)

/** The semantically relevant values extracted from the array-shaped wire frame. */
data class QuoteFrameDto(
    val event: String?,
    val payload: QuotePayloadDto?,
)

@JsonClass(generateAdapter = false)
data class TopSecuritiesResponseDto(
    @param:Json(name = "tickers") val tickers: List<Any?>? = null,
    @param:Json(name = "details") val details: Map<String, TopSecurityDetailsDto>? = null,
    @param:Json(name = "code") val code: Any? = null,
    @param:Json(name = "error") val error: Any? = null,
    @param:Json(name = "errMsg") val errorMessage: Any? = null,
)

@JsonClass(generateAdapter = false)
data class TopSecurityDetailsDto(
    @param:Json(name = "name") val name: Any? = null,
)
