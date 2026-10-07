package com.grch.tradernettestasignment.feature.quotes.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grch.tradernettestasignment.feature.quotes.api.domain.FixedTickers
import com.grch.tradernettestasignment.feature.quotes.api.domain.Quote
import com.grch.tradernettestasignment.feature.quotes.api.domain.QuoteRepository
import com.grch.tradernettestasignment.feature.quotes.api.domain.TopSecuritiesDataSource
import com.grch.tradernettestasignment.feature.quotes.api.domain.TopSecuritiesResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the quotes screen state, active ticker set, and collection lifecycle.
 *
 * The ViewModel owns whether the stream is collected: composables render state
 * and dispatch [QuotesAction]s, and never create or hold the socket. Collection
 * runs only while the app is foreground AND the screen is visible; either turning
 * `false` cancels collection. Socket retries are owned by the data layer.
 *
 * Source policy (agreed): the top-securities API is requested by default every
 * time the quotes screen opens (the rising edge of
 * [QuotesAction.OnScreenVisibilityChanged]). While the request is in flight the
 * current active set (the fixed list on first open) keeps rendering. A non-empty
 * success replaces the active set and rows in API order; any failure or an empty
 * response keeps the exact fixed list as the active set. There is no toggle,
 * persisted preference, or build flag. Changing the active set cancels and
 * reconnects/resubscribes the same verified one-frame format.
 */
@HiltViewModel
class QuotesViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val topSecuritiesDataSource: TopSecuritiesDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(QuotesState())
    val state: StateFlow<QuotesState> = _state.asStateFlow()

    private var isForeground = false
    private var isScreenVisible = false
    private var streamJob: Job? = null
    private var sourceJob: Job? = null
    private var activeTickers: List<String> = FixedTickers.ALL
    private val flashJobs = mutableMapOf<String, Job>()

    fun onAction(action: QuotesAction) {
        when (action) {
            is QuotesAction.OnAppForegroundChanged -> {
                isForeground = action.isForeground
                updateStreaming()
            }

            is QuotesAction.OnScreenVisibilityChanged -> {
                val wasVisible = isScreenVisible
                isScreenVisible = action.isVisible
                // Screen-open edge: the agreed default invocation point. Request a
                // fresh API set every time the screen opens; the current set keeps
                // rendering until the response is applied.
                if (action.isVisible && !wasVisible) requestTopSecurities()
                updateStreaming()
            }
        }
    }

    private val shouldStream: Boolean
        get() = isForeground && isScreenVisible

    /**
     * Requests the top-securities API for a fresh active ticker set. Runs once per
     * screen open; a newer open cancels an in-flight request. On a non-empty
     * success the API IDs replace the active set and rows in API order; on any
     * failure or an empty response the exact fixed list remains the active set.
     */
    private fun requestTopSecurities() {
        sourceJob?.cancel()
        sourceJob = viewModelScope.launch {
            when (val result = topSecuritiesDataSource.fetchTopSecurities()) {
                is TopSecuritiesResult.Success -> {
                    Log.d(TAG, "top-securities applied: ${result.securities.size} tickers")
                    applyActiveSet(
                        tickers = result.securities.map { it.ticker },
                        names = result.securities
                            .mapNotNull { ref -> ref.name?.let { ref.ticker to it } }
                            .toMap(),
                        source = QuoteSource.TOP_SECURITIES,
                    )
                }

                is TopSecuritiesResult.Failure -> {
                    // Spec: any failure or an empty response falls back to the
                    // exact fixed list without blocking the board.
                    Log.d(TAG, "top-securities fallback: ${result.error}")
                    applyActiveSet(
                        tickers = FixedTickers.ALL,
                        names = emptyMap(),
                        source = QuoteSource.FIXED,
                    )
                }
            }
        }
    }

    /**
     * Replaces the active ticker set and the rows (in the supplied order), and
     * cancels/reconnects the subscription only when the set actually changes so
     * the fallback path does not churn an unchanged subscription. Existing quote
     * values for tickers that remain are carried over; API `names` only fill a row
     * that has no name yet.
     */
    private fun applyActiveSet(
        tickers: List<String>,
        names: Map<String, String>,
        source: QuoteSource,
    ) {
        val changed = tickers != activeTickers
        activeTickers = tickers
        val existing = _state.value.rows.associateBy { it.ticker }
        val rows = tickers.map { ticker ->
            val prior = existing[ticker]
            when {
                prior == null -> QuoteRow(ticker = ticker, name = names[ticker])
                prior.name == null && names.containsKey(ticker) -> prior.copy(name = names[ticker])
                else -> prior
            }
        }
        _state.update { it.copy(rows = rows, source = source) }
        if (changed) restartStreaming()
    }

    private fun updateStreaming() {
        if (shouldStream) startStreaming() else stopStreaming()
    }

    /** Cancels current collection and starts a fresh one if streaming. */
    private fun restartStreaming() {
        streamJob?.cancel()
        streamJob = null
        _state.update { it.copy(isStreaming = false) }
        updateStreaming()
    }

    private fun startStreaming() {
        if (streamJob?.isActive == true) return
        _state.update { it.copy(isStreaming = true) }
        streamJob = viewModelScope.launch {
            try {
                quoteRepository.observeQuotes(activeTickers).collect(::mergeQuote)
            } finally {
                _state.update { it.copy(isStreaming = false) }
            }
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
        // Spec: never add a ticker outside the current active set.
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
        const val TAG = "QuotesViewModel"
        const val FLASH_DURATION_MS = 300L
    }
}
