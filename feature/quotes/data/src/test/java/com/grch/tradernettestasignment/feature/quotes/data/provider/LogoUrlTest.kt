package com.grch.tradernettestasignment.feature.quotes.data.provider

import com.grch.tradernettestasignment.feature.quotes.data.remote.TradernetUrls
import org.junit.Assert.assertEquals
import org.junit.Test

class LogoUrlTest {
    private val provider = TradernetSecurityLogoUrlProvider()

    @Test
    fun providerBuildsDocumentedUrlWithLowercasedTicker() {
        assertEquals(
            "https://tradernet.com/logos/get-logo-by-ticker?ticker=sber",
            provider.logoUrl("SBER"),
        )
    }

    @Test
    fun providerDelegatesToTradernetUrls() {
        assertEquals(TradernetUrls.logoUrl("GAZP.US"), provider.logoUrl("GAZP.US"))
    }

    @Test
    fun suffixAndDotsArePreservedAndNotEncoded() {
        assertEquals(
            "https://tradernet.com/logos/get-logo-by-ticker?ticker=aapl.us",
            TradernetUrls.logoUrl("AAPL.US"),
        )
    }

    @Test
    fun alreadyLowercaseAndEmptyTickers() {
        assertEquals("https://tradernet.com/logos/get-logo-by-ticker?ticker=abc", TradernetUrls.logoUrl("abc"))
        assertEquals("https://tradernet.com/logos/get-logo-by-ticker?ticker=", TradernetUrls.logoUrl(""))
    }

    @Test
    fun endpointConstants() {
        assertEquals("https://tradernet.com/", TradernetUrls.BASE_URL)
        assertEquals("wss://wss.tradernet.com/", TradernetUrls.SOCKET_URL)
    }
}
