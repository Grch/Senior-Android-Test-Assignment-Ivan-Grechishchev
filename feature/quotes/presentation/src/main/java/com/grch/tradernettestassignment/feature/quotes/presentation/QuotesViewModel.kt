package com.grch.tradernettestassignment.feature.quotes.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grch.tradernettestassignment.feature.quotes.domain.model.FixedTickers
import com.grch.tradernettestassignment.feature.quotes.domain.model.Quote
import com.grch.tradernettestassignment.feature.quotes.domain.repository.QuoteRepository
import com.grch.tradernettestassignment.feature.quotes.domain.model.QuoteStreamEvent
import com.grch.tradernettestassignment.feature.quotes.domain.provider.SecurityLogoUrlProvider
import com.grch.tradernettestassignment.feature.quotes.domain.repository.TopSecuritiesDataSource
import com.grch.tradernettestassignment.feature.quotes.domain.model.TopSecuritiesResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the quotes screen state and the stream collection, which runs only while the screen is started.
 * The fixed watchlist is always active; tickers from the top-securities API (requested once) are appended.
 * [BoardStatus.ERROR] is set only on [QuoteStreamEvent.ConnectionLost], i.e. once the data layer gave up.
 */
@HiltViewModel
class QuotesViewModel @Inject constructor(
    private val quoteRepository: QuoteRepository,
    private val topSecuritiesDataSource: TopSecuritiesDataSource,
    private val logoUrlProvider: SecurityLogoUrlProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(
        QuotesState(rows = FixedTickers.ALL.map { newRow(it, name = null) }),
    )
    internal val state: StateFlow<QuotesState> = _state.asStateFlow()

    private val isStarted = MutableStateFlow(false)
    private val activeTickers = MutableStateFlow(FixedTickers.ALL)
    private val retries = MutableStateFlow(0)
    private var sourceJob: Job? = null

    init {
        requestTopSecurities()
        viewModelScope.launch {
            combine(isStarted, activeTickers, retries) { started, tickers, _ ->
                tickers.takeIf { started }
            }.collectLatest { tickers ->
                // Stopped: the cancelled collection already cleared isStreaming.
                if (tickers == null) return@collectLatest
                _state.update { it.copy(isStreaming = true) }
                try {
                    quoteRepository.observeQuotes(tickers).collect(::handleStreamEvent)
                } finally {
                    _state.update { it.copy(isStreaming = false) }
                }
            }
        }
    }

    /**
     * `true` on ON_START, `false` when the app is minimized or the screen is left. A configuration
     * change must not produce `false`: the ViewModel keeps the stream across it.
     */
    fun onScreenStarted(isStarted: Boolean) {
        this.isStarted.value = isStarted
    }

    /** Retry from the error placeholder: reconnects immediately and dims the board until the outcome is known. */
    fun onRetryClick() {
        _state.update { it.copy(boardStatus = BoardStatus.RECONNECTING) }
        retries.update { it + 1 }
    }

    /** Requests the top-securities API once and appends tickers not already in [FixedTickers.ALL]. */
    private fun requestTopSecurities() {
        sourceJob?.cancel()
        sourceJob = viewModelScope.launch {
            when (val result = topSecuritiesDataSource.fetchTopSecurities()) {
                is TopSecuritiesResult.Success -> {
                    // Keep every API name (even for fixed tickers) so applyActiveSet can fill blank names.
                    val names = result.securities
                        .mapNotNull { ref -> ref.name?.let { ref.ticker to it } }
                        .toMap()
                    val fixedSet = FixedTickers.ALL.toHashSet()
                    val additions = result.securities
                        .map { it.ticker }
                        .filter { it !in fixedSet }
                        .distinct()
                    if (additions.isEmpty()) {
                        Log.d(TAG, "top-securities: no tickers beyond the fixed list")
                        applyActiveSet(
                            tickers = FixedTickers.ALL,
                            names = names,
                            source = QuoteSource.FIXED,
                        )
                    } else {
                        Log.d(TAG, "top-securities applied: ${additions.size} tickers added")
                        applyActiveSet(
                            tickers = FixedTickers.ALL + additions,
                            names = names,
                            source = QuoteSource.AUGMENTED,
                        )
                    }
                }

                is TopSecuritiesResult.Failure -> {
                    // Any failure falls back to the fixed list without blocking the board.
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

    /** Replaces the active ticker set and rows; quote values of remaining tickers are kept, API names only fill blanks. */
    private fun applyActiveSet(
        tickers: List<String>,
        names: Map<String, String>,
        source: QuoteSource,
    ) {
        val existing = _state.value.rows.associateBy { it.ticker }
        val rows = tickers.map { ticker ->
            val prior = existing[ticker]
            when {
                prior == null -> newRow(ticker, name = names[ticker])
                prior.name == null && names.containsKey(ticker) -> prior.copy(name = names[ticker])
                else -> prior
            }
        }
        _state.update { it.copy(rows = rows, source = source) }
        // StateFlow equality dedupes, so the fallback path doesn't churn an unchanged subscription.
        activeTickers.value = tickers
    }

    private fun newRow(ticker: String, name: String?): QuoteRow =
        QuoteRow(ticker = ticker, logoUrl = logoUrlProvider.logoUrl(ticker), name = name)

    /**
     * Routes a stream event. [QuoteStreamEvent.Disconnected] is ignored (the data layer is reconnecting);
     * [QuoteStreamEvent.Connected] clears the error and reconnecting states; [QuoteStreamEvent.ConnectionLost]
     * is terminal and shows the error placeholder.
     */
    private fun handleStreamEvent(event: QuoteStreamEvent) {
        when (event) {
            is QuoteStreamEvent.Update -> mergeQuote(event.quote)
            QuoteStreamEvent.Connected ->
                _state.update { it.copy(boardStatus = BoardStatus.CONTENT) }
            QuoteStreamEvent.Disconnected -> Unit
            QuoteStreamEvent.ConnectionLost ->
                _state.update { it.copy(boardStatus = BoardStatus.ERROR) }
        }
    }

    private fun mergeQuote(quote: Quote) {
        _state.update { state ->
            val rowIndex = state.rows.indexOfFirst { it.ticker == quote.ticker }
            // An update that matches no row is a no-op.
            if (rowIndex == -1) return@update state
            val row = state.rows[rowIndex]
            val direction = directionalChange(row.lastPrice, quote.lastPrice)
            val changed = direction != FlashDirection.NONE
            val merged = row.copy(
                exchange = quote.exchange ?: row.exchange,
                name = quote.name ?: row.name,
                lastPrice = quote.lastPrice ?: row.lastPrice,
                change = quote.change ?: row.change,
                percentChange = quote.percentChange ?: row.percentChange,
                previousClose = quote.previousClose ?: row.previousClose,
                minStep = quote.minStep ?: row.minStep,
                flash = if (changed) direction else row.flash,
                flashTick = if (changed) row.flashTick + 1 else row.flashTick,
            )
            val rows = state.rows.toMutableList().apply { this[rowIndex] = merged }
            state.copy(rows = rows, boardStatus = BoardStatus.CONTENT)
        }
    }

    private fun directionalChange(previous: Double?, next: Double?): FlashDirection {
        if (previous == null || next == null || previous == next) return FlashDirection.NONE
        return if (next > previous) FlashDirection.UP else FlashDirection.DOWN
    }

    private companion object {
        const val TAG = "QuotesViewModel"
    }
}
