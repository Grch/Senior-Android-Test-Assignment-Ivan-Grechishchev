package com.grch.tradernettestassignment.feature.quotes.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `q` frame payload (index 1). Everything except the ticker is nullable: later frames are partial
 * updates, and `min_step` arrives only on a ticker's first frame.
 */
@JsonClass(generateAdapter = true)
internal data class QuotePayloadDto(
    @param:Json(name = "c") val ticker: String? = null,
    @param:Json(name = "ltr") val exchange: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "ltp") val lastPrice: Double? = null,
    @param:Json(name = "chg") val change: Double? = null,
    @param:Json(name = "pcp") val percentChange: Double? = null,
    @param:Json(name = "pp") val previousClose: Double? = null,
    @param:Json(name = "min_step") val minStep: Double? = null,
)

/** The semantically relevant values extracted from the array-shaped wire frame. */
internal data class QuoteFrameDto(
    val event: String?,
    val payload: QuotePayloadDto?,
)

@JsonClass(generateAdapter = true)
internal data class TopSecuritiesResponseDto(
    @param:Json(name = "tickers") val tickers: List<Any?>? = null,
    @param:Json(name = "details") val details: Map<String, TopSecurityDetailsDto>? = null,
    @param:Json(name = "code") val code: Any? = null,
    @param:Json(name = "error") val error: Any? = null,
    @param:Json(name = "errMsg") val errorMessage: Any? = null,
)

@JsonClass(generateAdapter = true)
internal data class TopSecurityDetailsDto(
    @param:Json(name = "name") val name: Any? = null,
)
