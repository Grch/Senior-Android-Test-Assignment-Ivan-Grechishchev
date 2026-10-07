package com.grch.tradernettestasignment.feature.quotes.impl.domain

/**
 * A single security quote update from the Tradernet `q` carrier.
 *
 * Every field except [ticker] is nullable because Tradernet sends partial
 * updates: later frames only carry the fields that changed. Consumers merge
 * non-null fields into the row keyed by [ticker].
 *
 * Field names map to the documented Tradernet quote fields
 * (`https://tradernet.com/tradernet-api/quotes-get-changes/?site_lang=en`):
 * `c` -> [ticker], `ltr` -> [exchange], `name` -> [name], `ltp` -> [lastPrice],
 * `chg` -> [change] (signed points vs previous close), `pcp` -> [percentChange]
 * (percent vs previous close) and `pp` -> [previousClose].
 */
data class Quote(
    val ticker: String,
    val exchange: String? = null,
    val name: String? = null,
    val lastPrice: Double? = null,
    val change: Double? = null,
    val percentChange: Double? = null,
    val previousClose: Double? = null,
)
