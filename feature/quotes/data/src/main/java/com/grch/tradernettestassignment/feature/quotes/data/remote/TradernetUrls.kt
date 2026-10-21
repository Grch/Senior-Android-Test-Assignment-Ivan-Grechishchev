package com.grch.tradernettestassignment.feature.quotes.data.remote

/** Fixed unauthenticated Tradernet endpoints shared by the network module. */
internal object TradernetUrls {
    const val BASE_URL: String = "https://tradernet.com/"
    const val SOCKET_URL: String = "wss://wss.tradernet.com/"
    private const val LOGO_PATH: String = "logos/get-logo-by-ticker?ticker="

/**
     * Logo URL for the lowercased [ticker]. Tickers without a logo get a 200 with a 1x1 transparent GIF,
     * not an HTTP error.
     */
    fun logoUrl(ticker: String): String = BASE_URL + LOGO_PATH + ticker.lowercase()
}
