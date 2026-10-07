package com.grch.tradernettestasignment.feature.quotes.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grch.tradernettestasignment.feature.quotes.api.QuotesEntryPoint

/**
 * Concrete [QuotesEntryPoint] binding the public boundary to [QuotesRoot].
 *
 * The `:app` composition root is the only module that constructs this class and
 * exposes it as the API contract.
 */
class DefaultQuotesEntryPoint : QuotesEntryPoint {
    @Composable
    override fun Content(modifier: Modifier) {
        QuotesRoot(modifier = modifier)
    }
}
