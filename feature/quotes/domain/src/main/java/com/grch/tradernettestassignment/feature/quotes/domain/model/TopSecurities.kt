package com.grch.tradernettestassignment.feature.quotes.domain.model

/** A security reference from the top-securities endpoint; [name] comes from the response's `details` map. */
data class TopSecurityRef(
    val ticker: String,
    val name: String? = null,
)

/** Outcome of a top-securities fetch: [Success] only for a non-empty ticker list, otherwise [Failure]. */
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
