package com.grch.tradernettestassignment.feature.quotes.domain.repository

import com.grch.tradernettestassignment.feature.quotes.domain.model.TopSecuritiesResult

/** Fetches the current ordered top-securities set from the remote service. */
interface TopSecuritiesDataSource {
    suspend fun fetchTopSecurities(): TopSecuritiesResult
}
