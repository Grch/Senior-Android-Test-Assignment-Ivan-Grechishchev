package com.grch.tradernettestasignment.feature.quotes.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grch.tradernettestasignment.feature.quotes.impl.data.QuoteSocketDataSource
import com.grch.tradernettestasignment.feature.quotes.impl.data.TradernetQuoteSocketDataSource
import com.grch.tradernettestasignment.feature.quotes.impl.domain.Quote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Owns the quotes screen state and the live socket lifecycle.
 *
 * The ViewModel is the only owner of the stream: composables render state and
 * dispatch [QuotesAction]s, and never create or hold the socket. The stream runs
 * only while the app is foreground AND the screen is visible; either turning
 * `false` cancels the stream and its retry loop.
 */
class QuotesViewModel(
    private val dataSource: QuoteSocketDataSource = TradernetQuoteSocketDataSource(),
) : ViewModel() {

    private val _state = MutableStateFlow(QuotesState())
    val state: StateFlow<QuotesState> = _state.asStateFlow()

    private var isForeground = false
    private var isScreenVisible = false
    private var streamJob: Job? = null
    private val flashJobs = mutableMapOf<String, Job>()

    fun onAction(action: QuotesAction) {
        when (action) {
            is QuotesAction.OnAppForegroundChanged -> {
                isForeground = action.isForeground
                updateStreaming()
            }

            is QuotesAction.OnScreenVisibilityChanged -> {
                isScreenVisible = action.isVisible
                updateStreaming()
            }
        }
    }

    private val shouldStream: Boolean
        get() = isForeground && isScreenVisible

    private fun updateStreaming() {
        if (shouldStream) startStreaming() else stopStreaming()
    }

    private fun startStreaming() {
        if (streamJob?.isActive == true) return
        _state.update { it.copy(isStreaming = true) }
        streamJob = viewModelScope.launch {
            while (isActive && shouldStream) {
                try {
                    dataSource.observeQuotes().collect { quote -> mergeQuote(quote) }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    // Transient socket failure: fall through to the reconnect delay.
                }
                if (!shouldStream) break
                delay(RECONNECT_DELAY_MS)
            }
            _state.update { it.copy(isStreaming = false) }
        }
    }

    private fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        _state.update { it.copy(isStreaming = false) }
    }

    private fun mergeQuote(quote: Quote) {
        val current = _state.value
        val index = current.rows.indexOfFirst { it.ticker == quote.ticker }
        // Spec: never add a ticker outside the fixed list.
        if (index == -1) return

        val previous = current.rows[index]
        val direction = directionalChange(previous.lastPrice, quote.lastPrice)

        _state.update { state ->
            val rowIndex = state.rows.indexOfFirst { it.ticker == quote.ticker }
            if (rowIndex == -1) return@update state
            val row = state.rows[rowIndex]
            val merged = row.copy(
                exchange = quote.exchange ?: row.exchange,
                name = quote.name ?: row.name,
                lastPrice = quote.lastPrice ?: row.lastPrice,
                change = quote.change ?: row.change,
                percentChange = quote.percentChange ?: row.percentChange,
                previousClose = quote.previousClose ?: row.previousClose,
                flash = if (direction != FlashDirection.NONE) direction else row.flash,
            )
            val rows = state.rows.toMutableList().apply { this[rowIndex] = merged }
            state.copy(rows = rows)
        }

        if (direction != FlashDirection.NONE) scheduleFlashClear(quote.ticker)
    }

    private fun directionalChange(previous: Double?, next: Double?): FlashDirection {
        if (previous == null || next == null || previous == next) return FlashDirection.NONE
        return if (next > previous) FlashDirection.UP else FlashDirection.DOWN
    }

    private fun scheduleFlashClear(ticker: String) {
        flashJobs.remove(ticker)?.cancel()
        flashJobs[ticker] = viewModelScope.launch {
            delay(FLASH_DURATION_MS)
            _state.update { state ->
                val index = state.rows.indexOfFirst { it.ticker == ticker }
                if (index == -1) return@update state
                val row = state.rows[index]
                if (row.flash == FlashDirection.NONE) return@update state
                val rows = state.rows.toMutableList().apply {
                    this[index] = row.copy(flash = FlashDirection.NONE)
                }
                state.copy(rows = rows)
            }
            flashJobs.remove(ticker)
        }
    }

    private companion object {
        const val RECONNECT_DELAY_MS = 2_000L
        const val FLASH_DURATION_MS = 300L
    }
}
