package com.grch.tradernettestasignment.feature.quotes.impl

/**
 * Lifecycle signals handled by [QuotesViewModel].
 *
 * The socket runs only when the app is foreground AND the screen is visible, so
 * both signals are tracked independently: a transition of either to `false`
 * stops the stream, and only the rising edge back to `true` restarts it.
 */
sealed interface QuotesAction {
    data class OnAppForegroundChanged(val isForeground: Boolean) : QuotesAction
    data class OnScreenVisibilityChanged(val isVisible: Boolean) : QuotesAction
}
