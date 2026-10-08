package com.grch.tradernettestassignment.feature.quotes.data.provider

import com.grch.tradernettestassignment.feature.quotes.data.remote.TradernetUrls
import com.grch.tradernettestassignment.feature.quotes.domain.provider.SecurityLogoUrlProvider
import javax.inject.Inject

class TradernetSecurityLogoUrlProvider @Inject constructor() : SecurityLogoUrlProvider {
    override fun logoUrl(ticker: String): String = TradernetUrls.logoUrl(ticker)
}
