package com.grch.tradernettestasignment.feature.quotes.impl

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Owns the quotes screen state and orchestrates actions.
 *
 * This is a placeholder for the foundation: no socket, repository, or data
 * source is owned here yet. The `live-quotes-board` change fills in the real
 * stream orchestration while keeping this state-ownership contract.
 */
class QuotesViewModel : ViewModel() {

    private val _state = MutableStateFlow(QuotesState())
    val state: StateFlow<QuotesState> = _state.asStateFlow()

    fun onAction(action: QuotesAction) {
        when (action) {
            QuotesAction.OnPlaceholderAction -> _state.update { it.copy(isActionDispatched = true) }
        }
    }
}
