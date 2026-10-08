package com.grch.tradernettestassignment.feature.quotes.domain.model

/**
 * A single security reference sourced from the top-securities REST endpoint.
 *
 * The endpoint supplies ticker IDs (and optional display names) only; live quote
 * values continue to arrive over the existing WebSocket stream. [name] is
 * optional name metadata sourced from the response's `details` map.
 */
data class TopSecurityRef(
    val ticker: String,
    val name: String? = null,
)

/**
 * Outcome of a top-securities fetch.
 *
 * [Success] is returned only for a non-empty parsed `tickers` list. Every other
 * outcome is [Failure] carrying the specific [TopSecuritiesError], so the caller
 * can distinguish transport, HTTP, API-declared, parse, and empty failures from
 * non-empty success.
 */
sealed interface TopSecuritiesResult {
    data class Success(val securities: List<TopSecurityRef>) : TopSecuritiesResult
    data class Failure(val error: TopSecuritiesError) : TopSecuritiesResult
}

/** The distinct ways a top-securities fetch can fail. */
enum class TopSecuritiesError {
    /** The request could not reach the server (I/O, DNS, timeout). */
    TRANSPORT,

    /** The server responded with a non-success HTTP status. */
    HTTP,

    /** The server returned the documented failure shape `code`/`error`/`errMsg`. */
    API,

    /** The response body could not be parsed into the documented shape. */
    PARSE,

    /** The response parsed correctly but contained no tickers. */
    EMPTY,
}
