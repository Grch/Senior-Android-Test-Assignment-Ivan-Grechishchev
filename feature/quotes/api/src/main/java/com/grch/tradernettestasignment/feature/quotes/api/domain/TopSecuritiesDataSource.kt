package com.grch.tradernettestasignment.feature.quotes.api.domain

/** Fetches the current ordered top-securities set from the remote service. */
interface TopSecuritiesDataSource {
    suspend fun fetchTopSecurities(): TopSecuritiesResult
}
