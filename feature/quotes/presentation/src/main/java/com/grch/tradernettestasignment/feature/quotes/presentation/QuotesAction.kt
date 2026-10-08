package com.grch.tradernettestasignment.feature.quotes.presentation

/**
 * Lifecycle signal handled by [QuotesViewModel].
 *
 * [OnScreenStarted] is the single lifecycle signal: `true` when the host reaches
 * ON_START, `false` when it stops because the app was minimized or the screen was
 * left. A configuration change (rotation) must not produce `false`: the ViewModel
 * survives it, so the socket is kept and the recreated Activity's `true` is a
 * no-op because starting an already-running stream is idempotent.
 *
 * The top-securities source is not tied to this signal: the ViewModel requests it
 * once, when it is created. There is no source-selection action, toggle, or
 * persisted preference.
 *
 * [OnRetryClicked] is dispatched only from the error placeholder's "Try again"
 * button, shown when the board has never received any data and the socket is
 * disconnected.
 */
sealed interface QuotesAction {
    data class OnScreenStarted(val isStarted: Boolean) : QuotesAction
    data object OnRetryClicked : QuotesAction
}
