package com.grch.tradernettestassignment.feature.quotes.domain.provider

/** Resolves the logo image URL for a security, so presentation never knows the backend's URL scheme. */
interface SecurityLogoUrlProvider {
    fun logoUrl(ticker: String): String
}
