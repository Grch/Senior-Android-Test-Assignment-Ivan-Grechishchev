package com.grch.tradernettestassignment.feature.quotes.data.remote

/** Fixed unauthenticated Tradernet endpoints shared by the network module. */
internal object TradernetUrls {
    const val BASE_URL: String = "https://tradernet.com/"
    const val SOCKET_URL: String = "wss://wss.tradernet.com/"
    private const val LOGO_PATH: String = "logos/get-logo-by-ticker?ticker="

    /**
     * Builds the security-logo URL from the documented prefix plus the
     * lowercased [ticker]. No other host, path, or query parameter is added.
     * The ticker is lowercased but not URL-encoded (fine for real exchange tickers).
     *
     * Verified behavior: a ticker with a real logo returns `image/png`. A
     * ticker with no logo still responds `200 OK`, but with a 1x1 fully
     * transparent `image/gif` placeholder rather than an HTTP error — callers
     * must not treat that response as a failure; render it the same as a real
     * logo and let it be visually transparent.
     */
    fun logoUrl(ticker: String): String = BASE_URL + LOGO_PATH + ticker.lowercase()
}
