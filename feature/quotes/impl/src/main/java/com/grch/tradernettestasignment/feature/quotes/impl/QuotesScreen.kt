package com.grch.tradernettestasignment.feature.quotes.impl

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import java.math.BigDecimal

private val PositiveGreen = Color(0xFF1E9E5A)
private val NegativeRed = Color(0xFFD64545)
private const val PLACEHOLDER = "—"
private const val FLASH_ANIMATION_MS = 150

/**
 * Stateful entry to the quotes screen. Holds the [QuotesViewModel], reports app
 * foreground and screen visibility as actions, observes state, and forwards
 * actions. Hilt supplies the ViewModel and its API/data dependencies.
 */
@Composable
fun QuotesRoot(
    modifier: Modifier = Modifier,
    viewModel: QuotesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Screen visibility: the feature is visible while this composition exists.
    DisposableEffect(viewModel) {
        viewModel.onAction(QuotesAction.OnScreenVisibilityChanged(true))
        onDispose { viewModel.onAction(QuotesAction.OnScreenVisibilityChanged(false)) }
    }

    // App foreground/background: ON_START/ON_STOP of the host lifecycle.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAction(QuotesAction.OnAppForegroundChanged(true))
                Lifecycle.Event.ON_STOP -> viewModel.onAction(QuotesAction.OnAppForegroundChanged(false))
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        QuotesScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Pure renderer: it takes state and an action callback and owns no logic, socket,
 * repository, or data source.
 */
@Composable
fun QuotesScreen(
    state: QuotesState,
    onAction: (QuotesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = state.rows, key = { row -> row.ticker }) { row ->
            QuoteRowItem(row = row)
        }
    }
}

@Composable
private fun QuoteRowItem(
    row: QuoteRow,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.ticker,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val secondary = listOfNotNull(row.exchange, row.name).joinToString(" · ")
                if (secondary.isNotEmpty()) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                PercentBadge(
                    text = row.percentChange?.let(::formatPercent) ?: PLACEHOLDER,
                    sign = row.percentChange,
                    flash = row.flash,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = row.lastPrice?.let(::formatValue) ?: PLACEHOLDER,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    row.change?.let { change ->
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${formatSigned(change)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = signColor(change),
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/**
 * The percent-change value, highlighted on a tick: a quick color fade from
 * plain sign-colored text to a solid green/red pill with white text while
 * [flash] is active, then back. Only this badge highlights — not the row.
 */
@Composable
private fun PercentBadge(
    text: String,
    sign: Double?,
    flash: FlashDirection,
    modifier: Modifier = Modifier,
) {
    val targetBackground = when (flash) {
        FlashDirection.UP -> PositiveGreen
        FlashDirection.DOWN -> NegativeRed
        FlashDirection.NONE -> Color.Transparent
    }
    val background by animateColorAsState(
        targetValue = targetBackground,
        animationSpec = tween(FLASH_ANIMATION_MS),
        label = "percentBadgeBackground",
    )
    val targetTextColor = if (flash == FlashDirection.NONE) signColor(sign) else Color.White
    val textColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(FLASH_ANIMATION_MS),
        label = "percentBadgeText",
    )
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        modifier = modifier
            .background(background, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
@ReadOnlyComposable
private fun signColor(value: Double?): Color = when {
    value == null -> MaterialTheme.colorScheme.onSurfaceVariant
    value > 0 -> PositiveGreen
    value < 0 -> NegativeRed
    else -> MaterialTheme.colorScheme.onSurface
}

private fun formatPercent(value: Double): String = formatSigned(value) + "%"

private fun formatSigned(value: Double): String {
    val sign = if (value > 0) "+" else ""
    return sign + formatValue(value)
}

private fun formatValue(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

@Preview(showBackground = true)
@Composable
private fun QuotesScreenPreview() {
    MaterialTheme {
        QuotesScreen(
            state = QuotesState(
                rows = listOf(
                    QuoteRow(
                        ticker = "AAPL.US",
                        exchange = "FIX",
                        name = "Apple Inc.",
                        lastPrice = 333.23,
                        change = -0.87,
                        percentChange = -0.26,
                        flash = FlashDirection.DOWN,
                    ),
                    QuoteRow(
                        ticker = "SP500.IDX",
                        exchange = "FIX",
                        name = "Index S&P 500",
                        lastPrice = 7839.86,
                        change = 16.41,
                        percentChange = 0.21,
                        flash = FlashDirection.UP,
                    ),
                    QuoteRow(ticker = "AI.US"),
                ),
            ),
            onAction = {},
        )
    }
}
