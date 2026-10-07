package com.grch.tradernettestasignment.feature.quotes.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Stable public entry contract for the quotes feature.
 *
 * The implementation (`:feature:quotes:impl`) provides a concrete
 * [QuotesEntryPoint]; the `:app` composition root is the only module that binds
 * the implementation to this contract. Every other consumer integrates through
 * this API only.
 */
fun interface QuotesEntryPoint {
    /**
     * Renders the quotes feature.
     *
     * [modifier] is applied by the host so the feature fills the space provided
     * by the app entry point.
     */
    @Composable
    fun Content(modifier: Modifier)
}
