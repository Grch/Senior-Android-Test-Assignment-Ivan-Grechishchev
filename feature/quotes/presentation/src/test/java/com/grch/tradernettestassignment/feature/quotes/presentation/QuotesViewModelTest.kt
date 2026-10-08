package com.grch.tradernettestassignment.feature.quotes.presentation

import com.grch.tradernettestassignment.feature.quotes.domain.model.FixedTickers
import com.grch.tradernettestassignment.feature.quotes.domain.model.Quote
import com.grch.tradernettestassignment.feature.quotes.domain.model.QuoteStreamEvent
import com.grch.tradernettestassignment.feature.quotes.domain.model.TopSecuritiesError
import com.grch.tradernettestassignment.feature.quotes.domain.model.TopSecuritiesResult
import com.grch.tradernettestassignment.feature.quotes.domain.model.TopSecurityRef
import com.grch.tradernettestassignment.feature.quotes.domain.provider.SecurityLogoUrlProvider
import com.grch.tradernettestassignment.feature.quotes.domain.repository.QuoteRepository
import com.grch.tradernettestassignment.feature.quotes.domain.repository.TopSecuritiesDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuotesViewModelTest {

    private class FakeQuoteRepository : QuoteRepository {
        val requestedTickers = mutableListOf<List<String>>()
        var activeCollections = 0

        // Tests emit here; every active collection receives the events.
        val events = MutableSharedFlow<QuoteStreamEvent>(extraBufferCapacity = 64)

        override fun observeQuotes(tickers: List<String>): Flow<QuoteStreamEvent> = flow {
            requestedTickers += tickers
            activeCollections++
            try {
                emitAll(events)
            } finally {
                activeCollections--
            }
        }
    }

    private class FakeTopSecurities(
        var result: TopSecuritiesResult = TopSecuritiesResult.Failure(
            TopSecuritiesError.TRANSPORT,
        ),
    ) : TopSecuritiesDataSource {
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun fetchTopSecurities(): TopSecuritiesResult {
            calls++
            gate?.await()
            return result
        }
    }

    private object FakeLogos : SecurityLogoUrlProvider {
        override fun logoUrl(ticker: String): String = "logo://$ticker"
    }

    private val repository = FakeQuoteRepository()
    private val topSecurities = FakeTopSecurities()

    // One dispatcher (and so one scheduler) for both Dispatchers.Main and runTest,
    // so virtual-time delays advance consistently.
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun runTest(testBody: suspend TestScope.() -> Unit) =
        kotlinx.coroutines.test.runTest(testDispatcher, testBody = testBody)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        // Cancel every ViewModel scope (no production change needed: viewModelScope is public).
        createdViewModels.forEach { it.viewModelScope.cancel() }
        createdViewModels.clear()
        Dispatchers.resetMain()
    }

    private val createdViewModels = mutableListOf<QuotesViewModel>()

    private fun viewModel() =
        QuotesViewModel(repository, topSecurities, FakeLogos).also { createdViewModels += it }

    private val tickerA = FixedTickers.ALL[0]
    private val tickerB = FixedTickers.ALL[1]

    private fun emitQuote(ticker: String, price: Double? = null, change: Double? = null,
        percent: Double? = null, minStep: Double? = null) {
        check(repository.events.tryEmit(
            QuoteStreamEvent.Update(Quote(ticker, lastPrice = price, change = change,
                percentChange = percent, minStep = minStep)),
        ))
    }

    private fun emitEvent(event: QuoteStreamEvent) {
        check(repository.events.tryEmit(event))
    }

    private fun QuotesViewModel.row(ticker: String) = state.value.rows.first { it.ticker == ticker }

    @Test
    fun `does not stream before the screen is started`() = runTest {
        val vm = viewModel()

        assertFalse(vm.state.value.isStreaming)
        assertEquals(0, repository.activeCollections)
        assertTrue(repository.requestedTickers.isEmpty())
    }

    @Test
    fun `starting the screen starts streaming`() = runTest {
        val vm = viewModel()

        vm.onScreenStarted(true)

        assertTrue(vm.state.value.isStreaming)
        assertEquals(1, repository.activeCollections)
        assertEquals(listOf(FixedTickers.ALL), repository.requestedTickers)
    }

    @Test
    fun `stopping the screen cancels streaming`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)

        vm.onScreenStarted(false)

        assertFalse(vm.state.value.isStreaming)
        assertEquals(0, repository.activeCollections)
    }

    @Test
    fun `repeated start while streaming does not start a second collection`() = runTest {
        val vm = viewModel()

        vm.onScreenStarted(true)
        vm.onScreenStarted(true)

        assertEquals(1, repository.requestedTickers.size)
        assertEquals(1, repository.activeCollections)
        assertTrue(vm.state.value.isStreaming)
    }

    @Test
    fun `top securities is requested once regardless of start stop cycles`() = runTest {
        val vm = viewModel()

        repeat(3) {
            vm.onScreenStarted(true)
            vm.onScreenStarted(false)
        }

        assertEquals(1, topSecurities.calls)
    }

    @Test
    fun `seeded rows carry the provider logo url`() = runTest {
        val vm = viewModel()

        val rows = vm.state.value.rows
        assertEquals(FixedTickers.ALL, rows.map { it.ticker })
        assertTrue(rows.all { it.logoUrl == "logo://${it.ticker}" })
    }

    @Test
    fun `appended api tickers carry the provider logo url`() = runTest {
        topSecurities.result = TopSecuritiesResult.Success(
            listOf(TopSecurityRef("NEW.US", "New Corp"), TopSecurityRef(FixedTickers.ALL.first())),
        )

        val vm = viewModel()

        val rows = vm.state.value.rows
        assertEquals(FixedTickers.ALL + "NEW.US", rows.map { it.ticker })
        val appended = rows.last()
        assertEquals("logo://NEW.US", appended.logoUrl)
        assertEquals("New Corp", appended.name)
        assertEquals(QuoteSource.AUGMENTED, vm.state.value.source)
    }

    @Test
    fun `top securities additions while started yield one collection with the augmented set`() = runTest {
        val gate = CompletableDeferred<Unit>()
        topSecurities.gate = gate
        topSecurities.result = TopSecuritiesResult.Success(
            listOf(TopSecurityRef("NEW.US", "New Corp"), TopSecurityRef("OTHER.US")),
        )
        val vm = viewModel()
        vm.onScreenStarted(true)
        assertEquals(listOf(FixedTickers.ALL), repository.requestedTickers)

        gate.complete(Unit)

        val expected = FixedTickers.ALL + listOf("NEW.US", "OTHER.US")
        assertEquals(listOf(FixedTickers.ALL, expected), repository.requestedTickers)
        assertEquals(1, repository.activeCollections)
        assertTrue(vm.state.value.isStreaming)
    }

    @Test
    fun `retry while started restarts the collection with the same tickers`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)

        vm.onRetryClick()

        assertEquals(listOf(FixedTickers.ALL, FixedTickers.ALL), repository.requestedTickers)
        assertEquals(1, repository.activeCollections)
        assertTrue(vm.state.value.isStreaming)
    }

    @Test
    fun `retry while not started shows reconnecting but does not subscribe`() = runTest {
        val vm = viewModel()

        vm.onRetryClick()

        assertEquals(BoardStatus.RECONNECTING, vm.state.value.boardStatus)
        assertTrue(repository.requestedTickers.isEmpty())
        assertEquals(0, repository.activeCollections)
        assertFalse(vm.state.value.isStreaming)
    }

    @Test
    fun `disconnected before any update stays in content`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)

        emitEvent(QuoteStreamEvent.Disconnected)

        assertEquals(BoardStatus.CONTENT, vm.state.value.boardStatus)
    }

    @Test
    fun `disconnected after an update stays in content`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        emitQuote(tickerA, price = 1.0)

        emitEvent(QuoteStreamEvent.Disconnected)

        assertEquals(BoardStatus.CONTENT, vm.state.value.boardStatus)
    }

    @Test
    fun `connection lost before any update shows the error state`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)

        emitEvent(QuoteStreamEvent.ConnectionLost)

        assertEquals(BoardStatus.ERROR, vm.state.value.boardStatus)
    }

    @Test
    fun `connection lost after an update shows the error state`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        emitQuote(tickerA, price = 1.0)

        emitEvent(QuoteStreamEvent.ConnectionLost)

        assertEquals(BoardStatus.ERROR, vm.state.value.boardStatus)
    }

    @Test
    fun `update after error recovers to content without retry`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        emitEvent(QuoteStreamEvent.ConnectionLost)
        assertEquals(BoardStatus.ERROR, vm.state.value.boardStatus)

        emitQuote(tickerA, price = 42.5, percent = 1.5)

        assertEquals(BoardStatus.CONTENT, vm.state.value.boardStatus)
        assertEquals(42.5, vm.row(tickerA).lastPrice)
        assertEquals(1.5, vm.row(tickerA).percentChange)
        assertEquals(1, repository.requestedTickers.size)
    }

    @Test
    fun `connected after error recovers to content without retry`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        emitEvent(QuoteStreamEvent.ConnectionLost)
        assertEquals(BoardStatus.ERROR, vm.state.value.boardStatus)

        emitEvent(QuoteStreamEvent.Connected)

        assertEquals(BoardStatus.CONTENT, vm.state.value.boardStatus)
        assertEquals(1, repository.requestedTickers.size)
    }

    @Test
    fun `resubscribing after error stays in error until connected`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        emitEvent(QuoteStreamEvent.ConnectionLost)
        assertEquals(BoardStatus.ERROR, vm.state.value.boardStatus)

        vm.onScreenStarted(false)
        vm.onScreenStarted(true)

        assertEquals(2, repository.requestedTickers.size)
        assertEquals(1, repository.activeCollections)
        assertEquals(BoardStatus.ERROR, vm.state.value.boardStatus)

        emitEvent(QuoteStreamEvent.Connected)

        assertEquals(BoardStatus.CONTENT, vm.state.value.boardStatus)
    }

    @Test
    fun `retry from error shows reconnecting with a fresh collection`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        emitEvent(QuoteStreamEvent.ConnectionLost)
        assertEquals(BoardStatus.ERROR, vm.state.value.boardStatus)

        vm.onRetryClick()

        assertEquals(BoardStatus.RECONNECTING, vm.state.value.boardStatus)
        assertEquals(listOf(FixedTickers.ALL, FixedTickers.ALL), repository.requestedTickers)
        assertEquals(1, repository.activeCollections)
    }

    private fun QuotesViewModel.startAndRetryFromError() {
        onScreenStarted(true)
        emitEvent(QuoteStreamEvent.ConnectionLost)
        onRetryClick()
        assertEquals(BoardStatus.RECONNECTING, state.value.boardStatus)
    }

    @Test
    fun `disconnected during reconnecting stays reconnecting until connected`() = runTest {
        val vm = viewModel()
        vm.startAndRetryFromError()

        emitEvent(QuoteStreamEvent.Disconnected)
        assertEquals(BoardStatus.RECONNECTING, vm.state.value.boardStatus)

        emitEvent(QuoteStreamEvent.Connected)
        assertEquals(BoardStatus.CONTENT, vm.state.value.boardStatus)
    }

    @Test
    fun `connection lost during reconnecting returns to error`() = runTest {
        val vm = viewModel()
        vm.startAndRetryFromError()

        emitEvent(QuoteStreamEvent.ConnectionLost)

        assertEquals(BoardStatus.ERROR, vm.state.value.boardStatus)
    }

    @Test
    fun `update for a known ticker during reconnecting shows content`() = runTest {
        val vm = viewModel()
        vm.startAndRetryFromError()

        emitQuote(tickerA, price = 1.0)

        assertEquals(BoardStatus.CONTENT, vm.state.value.boardStatus)
    }

    @Test
    fun `update merges into the matching row only and ignores unknown tickers`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        val before = vm.state.value.rows

        emitQuote(tickerB, price = 10.5, change = 0.5, percent = 5.0, minStep = 0.01)
        emitQuote("UNKNOWN.XX", price = 99.0)

        val rows = vm.state.value.rows
        assertEquals(before.map { it.ticker }, rows.map { it.ticker })
        val updated = vm.row(tickerB)
        assertEquals(10.5, updated.lastPrice)
        assertEquals(0.5, updated.change)
        assertEquals(5.0, updated.percentChange)
        assertEquals(0.01, updated.minStep)
        rows.filter { it.ticker != tickerB }.forEach { row ->
            assertEquals(before.first { it.ticker == row.ticker }, row)
        }
    }

    @Test
    fun `first quote does not flash but later increase flashes up and decrease flashes down`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)

        emitQuote(tickerA, price = 10.0)
        assertEquals(FlashDirection.NONE, vm.row(tickerA).flash)
        assertEquals(0, vm.row(tickerA).flashTick)

        emitQuote(tickerA, price = 11.0)
        assertEquals(FlashDirection.UP, vm.row(tickerA).flash)
        assertEquals(1, vm.row(tickerA).flashTick)

        emitQuote(tickerA, price = 9.0)
        assertEquals(FlashDirection.DOWN, vm.row(tickerA).flash)
        assertEquals(2, vm.row(tickerA).flashTick)
    }

    @Test
    fun `an unchanged price leaves flash and flashTick untouched`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        emitQuote(tickerA, price = 10.0)
        emitQuote(tickerA, price = 11.0)

        emitQuote(tickerA, price = 11.0)

        assertEquals(FlashDirection.UP, vm.row(tickerA).flash)
        assertEquals(1, vm.row(tickerA).flashTick)
    }

    @Test
    fun `flash ticks are per row`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        emitQuote(tickerA, price = 10.0)
        emitQuote(tickerB, price = 20.0)

        emitQuote(tickerA, price = 11.0)
        emitQuote(tickerA, price = 12.0)
        emitQuote(tickerB, price = 19.0)

        assertEquals(2, vm.row(tickerA).flashTick)
        assertEquals(FlashDirection.UP, vm.row(tickerA).flash)
        assertEquals(1, vm.row(tickerB).flashTick)
        assertEquals(FlashDirection.DOWN, vm.row(tickerB).flash)
    }

    @Test
    fun `an unknown ticker update is a full no-op`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        val before = vm.state.value

        emitQuote("UNKNOWN.XX", price = 99.0)

        assertEquals(before, vm.state.value)
        emitEvent(QuoteStreamEvent.ConnectionLost)
        assertEquals(BoardStatus.ERROR, vm.state.value.boardStatus)
    }

    @Test
    fun `an unknown ticker update does not leave the error state`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        emitEvent(QuoteStreamEvent.ConnectionLost)
        assertEquals(BoardStatus.ERROR, vm.state.value.boardStatus)

        emitQuote("UNKNOWN.XX", price = 99.0)

        assertEquals(BoardStatus.ERROR, vm.state.value.boardStatus)
    }

    @Test
    fun `retry while stopped then start yields exactly one collection`() = runTest {
        val vm = viewModel()
        vm.onRetryClick()

        vm.onScreenStarted(true)

        assertEquals(1, repository.requestedTickers.size)
        assertEquals(1, repository.activeCollections)
    }

    @Test
    fun `top securities completing while stopped then start yields one augmented collection`() = runTest {
        val gate = CompletableDeferred<Unit>()
        topSecurities.gate = gate
        topSecurities.result = TopSecuritiesResult.Success(
            listOf(TopSecurityRef("NEW.US", "New Corp"), TopSecurityRef("OTHER.US")),
        )
        val vm = viewModel()
        gate.complete(Unit)
        assertTrue(repository.requestedTickers.isEmpty())

        vm.onScreenStarted(true)

        assertEquals(listOf(FixedTickers.ALL + listOf("NEW.US", "OTHER.US")), repository.requestedTickers)
        assertEquals(1, repository.activeCollections)
    }

    @Test
    fun `cancelling the view model scope tears down the collection`() = runTest {
        val vm = viewModel()
        vm.onScreenStarted(true)
        assertEquals(1, repository.activeCollections)

        vm.viewModelScope.cancel()

        assertEquals(0, repository.activeCollections)
    }
}
