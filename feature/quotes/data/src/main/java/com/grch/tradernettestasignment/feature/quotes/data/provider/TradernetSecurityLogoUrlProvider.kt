package com.grch.tradernettestasignment.feature.quotes.data.provider

import com.grch.tradernettestasignment.feature.quotes.data.remote.TradernetUrls
import com.grch.tradernettestasignment.feature.quotes.domain.provider.SecurityLogoUrlProvider
import javax.inject.Inject

class TradernetSecurityLogoUrlProvider @Inject constructor() : SecurityLogoUrlProvider {
    override fun logoUrl(ticker: String): String = TradernetUrls.logoUrl(ticker)
}
