package com.grch.tradernettestasignment.feature.quotes.impl

/**
 * Lifecycle signals handled by [QuotesViewModel].
 *
 * The socket runs only when the app is foreground AND the screen is visible, so
 * both signals are tracked independently: a transition of either to `false`
 * stops the stream, and only the rising edge back to `true` restarts it.
 *
 * The rising edge of [OnScreenVisibilityChanged] is also the agreed invocation
 * point for the top-securities source: the ViewModel requests the API every time
 * the quotes screen opens. There is no source-selection action, toggle, or
 * persisted preference.
 *
 * [OnRetryClicked] is dispatched only from the error placeholder's "Try again"
 * button, shown when the board has never received any data and the socket is
 * disconnected.
 */
sealed interface QuotesAction {
    data class OnAppForegroundChanged(val isForeground: Boolean) : QuotesAction
    data class OnScreenVisibilityChanged(val isVisible: Boolean) : QuotesAction
    data object OnRetryClicked : QuotesAction
}
