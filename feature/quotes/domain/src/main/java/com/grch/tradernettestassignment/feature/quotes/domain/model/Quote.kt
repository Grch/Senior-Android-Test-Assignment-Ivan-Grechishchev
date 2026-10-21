package com.grch.tradernettestassignment.feature.quotes.domain.model

/**
 * A partial quote update from the Tradernet `q` frame: every field except [ticker] is nullable and
 * only changed fields are present. `min_step` ([minStep]), [exchange] and [name] arrive only on a
 * ticker's first frame.
 */
data class Quote(
    val ticker: String,
    val exchange: String? = null,
    val name: String? = null,
    val lastPrice: Double? = null,
    val change: Double? = null,
    val percentChange: Double? = null,
    val previousClose: Double? = null,
    val minStep: Double? = null,
)
