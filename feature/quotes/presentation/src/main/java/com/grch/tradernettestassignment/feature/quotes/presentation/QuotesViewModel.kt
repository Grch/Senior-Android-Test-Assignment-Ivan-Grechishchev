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
 * Owns the quotes screen state, active ticker set, and collection lifecycle.
 *
 * The ViewModel decides whether the stream is collected; composables only render
 * state and forward events. Collection runs only while the screen is started
 * ([onScreenStarted]). Socket retries are owned by the data layer.
 *
 * Streaming is one reactive pipeline: started flag, active ticker set, and a
 * manual retry counter are combined, and every change restarts the collection
 * (`collectLatest` cancels the previous one) or leaves it stopped.
 *
 * The required [FixedTickers] watchlist is always part of the active set, in its
 * order. The top-securities API is requested once, when the ViewModel is created:
 * any ticker it returns that is not already in the fixed list is appended, in API
 * order, after the fixed rows. A failure, an empty response, or a response with
 * only known tickers leaves the fixed list as the whole active set. Changing the
 * active set cancels and resubscribes with the same one-frame request.
 *
 * [QuotesState.boardStatus] starts at [BoardStatus.CONTENT] (rows render
 * immediately, shimmering the fields they don't have yet) and moves to
 * [BoardStatus.ERROR] only on [QuoteStreamEvent.ConnectionLost], i.e. once the
 * data layer gave up reconnecting. A plain [QuoteStreamEvent.Disconnected] keeps
 * the last data while the data layer reconnects. Any merged quote or a
 * [QuoteStreamEvent.Connected] moves the status back to [BoardStatus.CONTENT]. A
 * manual retry moves it to [BoardStatus.RECONNECTING] until the socket connects
 * (or an update arrives), or the data layer gives up again.
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
    val state: StateFlow<QuotesState> = _state.asStateFlow()

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
                // Stopped: the previous collection was cancelled and its finally
                // already cleared isStreaming.
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
     * `true` when the host reaches ON_START, `false` when it stops because the app
     * was minimized or the screen was left. A configuration change must not produce
     * `false`: the ViewModel survives it, so the stream is kept and the recreated
     * Activity's `true` is a no-op (starting a running stream is idempotent).
     */
    fun onScreenStarted(isStarted: Boolean) {
        this.isStarted.value = isStarted
    }

    /**
     * Called from the error placeholder's retry button, shown after the data layer
     * gave up reconnecting. Reconnects right away, bypassing the data layer's retry
     * delay, and shows the board dimmed with a spinner (RECONNECTING) until
     * Connected/Update confirms the socket or ConnectionLost brings the error back.
     */
    fun onRetryClick() {
        _state.update { it.copy(boardStatus = BoardStatus.RECONNECTING) }
        retries.update { it + 1 }
    }

    /**
     * Requests the top-securities API to augment the fixed active ticker set.
     * Runs once per ViewModel. Any
     * distinct API ticker not already in [FixedTickers.ALL] is appended, in API
     * order, after the fixed rows; a failure, an empty response, or a response
     * with no tickers beyond the fixed list leaves the exact fixed list as the
     * active set. API `details` names are applied to matching rows (fixed or
     * appended) regardless of whether any ticker was actually appended.
     */
    private fun requestTopSecurities() {
        sourceJob?.cancel()
        sourceJob = viewModelScope.launch {
            when (val result = topSecuritiesDataSource.fetchTopSecurities()) {
                is TopSecuritiesResult.Success -> {
                    // All API-supplied names (including for tickers already in the
                    // fixed list, e.g. Russian blue chips) are kept so applyActiveSet
                    // can fill a blank fixed-row name; only the active ticker set
                    // itself is restricted to tickers beyond the fixed list.
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

    /**
     * Replaces the active ticker set and the rows (in the supplied order); the
     * stream pipeline resubscribes when the set changes. Existing quote
     * values for tickers that remain are carried over; API `names` only fill a row
     * that has no name yet.
     */
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
     * Routes a stream event: a [QuoteStreamEvent.Update] merges into its row.
     * [QuoteStreamEvent.Disconnected] is ignored — the data layer is reconnecting
     * and the board keeps showing its last data or shimmer (a manual
     * [BoardStatus.RECONNECTING] stays as is).
     * [QuoteStreamEvent.Connected] clears a prior error placeholder or reconnecting
     * spinner, so a successful (re)connect never leaves them over a live socket.
     * [QuoteStreamEvent.ConnectionLost] is terminal (the data layer gave up and
     * the stream completes), so the UI switches to the error placeholder
     * regardless of whether data ever arrived; the user can retry manually.
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
            // Never add a ticker outside the current active set; an update that
            // matches no row is a no-op.
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
