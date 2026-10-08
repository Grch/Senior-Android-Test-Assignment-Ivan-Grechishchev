package com.grch.tradernettestassignment.feature.quotes.presentation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.grch.tradernettestassignment.feature.quotes.domain.model.FixedTickers

private const val RECONNECTING_BOARD_ALPHA = 0.4f

/**
 * Stateful entry to the quotes screen. Holds the [QuotesViewModel], reports the
 * started/stopped lifecycle to it, observes state, and forwards the retry click.
 */
@Composable
fun QuotesRoot(
    modifier: Modifier = Modifier,
    viewModel: QuotesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // A rotation stops the old Activity, but the ViewModel survives it: keep the
    // socket instead of reconnecting. The new Activity's ON_START is then a no-op
    // because the started flag is unchanged.
    val activity = LocalActivity.current
    val onScreenStarted by rememberUpdatedState(viewModel::onScreenStarted)
    val onRetryClick by rememberUpdatedState(viewModel::onRetryClick)
    LifecycleStartEffect(Unit) {
        onScreenStarted(true)
        onStopOrDispose {
            if (activity?.isChangingConfigurations != true) {
                onScreenStarted(false)
            }
        }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        QuotesScreen(
            state = state,
            onRetryClick = onRetryClick,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/** Pure renderer: takes state and a retry callback; owns no logic, socket, or data source. */
@Composable
internal fun QuotesScreen(
    state: QuotesState,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.boardStatus) {
        BoardStatus.ERROR -> QuotesErrorBoard(
            onRetryClick = onRetryClick,
            modifier = modifier,
        )
        // One branch for both statuses so the board keeps its composition (scroll
        // position, loaded logos) across RECONNECTING <-> CONTENT.
        BoardStatus.CONTENT, BoardStatus.RECONNECTING -> {
            val reconnecting = state.boardStatus == BoardStatus.RECONNECTING
            Box(modifier = modifier.fillMaxSize()) {
                QuotesBoard(
                    state = state,
                    modifier = Modifier
                        .alpha(if (reconnecting) RECONNECTING_BOARD_ALPHA else 1f)
                        .then(if (reconnecting) Modifier.clearAndSetSemantics {} else Modifier),
                )
                if (reconnecting) {
                    val reconnectingDescription = stringResource(R.string.quotes_reconnecting)
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics {
                                contentDescription = reconnectingDescription
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuotesBoard(
    state: QuotesState,
    modifier: Modifier = Modifier,
) {
    // Tickers are known upfront (the fixed list, plus whatever the
    // top-securities augmentation already settled on), so the real rows
    // render immediately; a row only shimmers the specific fields it
    // hasn't received a quote for yet, rather than hiding the whole list
    // behind a generic loading screen. The shimmer brush is shared and
    // created only while at least one row still needs it.
    val shimmerBrush = if (state.rows.any { it.lastPrice == null }) rememberShimmerBrush() else null
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = state.rows, key = { row -> row.ticker }) { row ->
            QuoteRowItem(row = row, shimmerBrush = shimmerBrush)
        }
    }
}

/** Full-screen placeholder shown when the connection is lost after repeated failed reconnect attempts. */
@Composable
private fun QuotesErrorBoard(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.quotes_error_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetryClick) {
            Text(stringResource(R.string.quotes_error_retry))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuotesScreenPreview() {
    MaterialTheme {
        QuotesScreen(
            state = QuotesState(
                rows = listOf(
                    QuoteRow(
                        ticker = "AAPL.US",
                        logoUrl = "https://example.com/aapl.png",
                        exchange = "FIX",
                        name = "Apple Inc.",
                        lastPrice = 333.23,
                        change = -0.87,
                        percentChange = -0.26,
                        minStep = 0.01,
                        flash = FlashDirection.DOWN,
                        flashTick = 1,
                    ),
                    QuoteRow(
                        ticker = "SP500.IDX",
                        logoUrl = "https://example.com/sp500.png",
                        exchange = "FIX",
                        name = "Index S&P 500",
                        lastPrice = 7839.86,
                        change = 16.41,
                        percentChange = 0.21,
                        minStep = 0.01,
                        flash = FlashDirection.UP,
                        flashTick = 1,
                    ),
                    QuoteRow(
                        ticker = "TGKA",
                        logoUrl = "https://example.com/tgka.png",
                        exchange = "MCX",
                        name = "ПАО ТГК-1",
                        lastPrice = 0.004358,
                        change = 0.000006,
                        percentChange = 0.14,
                        minStep = 0.000002,
                    ),
                    QuoteRow(ticker = "AI.US", logoUrl = "https://example.com/ai.png"),
                ),
                boardStatus = BoardStatus.CONTENT,
            ),
            onRetryClick = {},
        )
    }
}

/** Cold start: every fixed-list row seeded but still pending (no quote yet). */
@Preview(showBackground = true)
@Composable
private fun QuotesScreenPendingPreview() {
    MaterialTheme {
        QuotesScreen(
            state = QuotesState(
                rows = FixedTickers.ALL.map {
                    QuoteRow(ticker = it, logoUrl = "https://example.com/logo.png")
                },
            ),
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuotesScreenErrorPreview() {
    MaterialTheme {
        QuotesScreen(
            state = QuotesState(boardStatus = BoardStatus.ERROR),
            onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuotesScreenReconnectingPreview() {
    MaterialTheme {
        QuotesScreen(
            state = QuotesState(
                rows = FixedTickers.ALL.map {
                    QuoteRow(ticker = it, logoUrl = "https://example.com/logo.png")
                },
                boardStatus = BoardStatus.RECONNECTING,
            ),
            onRetryClick = {},
        )
    }
}
