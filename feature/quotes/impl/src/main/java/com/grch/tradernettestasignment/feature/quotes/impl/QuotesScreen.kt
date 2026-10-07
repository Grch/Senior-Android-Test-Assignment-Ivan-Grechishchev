package com.grch.tradernettestasignment.feature.quotes.impl

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.grch.tradernettestasignment.feature.quotes.impl.data.TradernetUrls
import java.math.BigDecimal
import java.math.RoundingMode

private val PositiveGreen = Color(0xFF1E9E5A)
private val NegativeRed = Color(0xFFD64545)
private const val PLACEHOLDER = "—"
private const val FLASH_ANIMATION_MS = 150
private val LogoSize = 36.dp

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
    when (state.boardStatus) {
        BoardStatus.ERROR -> QuotesErrorBoard(
            onRetryClick = { onAction(QuotesAction.OnRetryClicked) },
            modifier = modifier,
        )
        BoardStatus.CONTENT -> {
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
    }
}

/** Full-screen placeholder shown when the socket has never delivered any data. */
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
            text = "Oops, seems we've got an error",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onRetryClick) {
            Text("Try again")
        }
    }
}

/**
 * A single watchlist row. While [row] has no [QuoteRow.lastPrice] yet (no quote
 * frame has arrived for it), the name/exchange line and the whole price area
 * shimmer using [shimmerBrush] instead of showing a dash — the ticker itself is
 * always known upfront, so only it and the logo render immediately either way.
 */
@Composable
private fun QuoteRowItem(
    row: QuoteRow,
    shimmerBrush: Brush?,
    modifier: Modifier = Modifier,
) {
    val isPending = row.lastPrice == null
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecurityLogo(ticker = row.ticker, modifier = Modifier.padding(end = 12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.ticker,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val secondary = listOfNotNull(row.exchange, row.name).joinToString(" · ")
                if (isPending) {
                    ShimmerBlock(
                        brush = shimmerBrush,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth(0.55f)
                            .height(12.dp),
                    )
                } else if (secondary.isNotEmpty()) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isPending) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBlock(brush = shimmerBrush, modifier = Modifier.width(52.dp).height(20.dp))
                    ShimmerBlock(brush = shimmerBrush, modifier = Modifier.width(72.dp).height(14.dp))
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    PercentBadge(
                        text = row.percentChange?.let(::formatPercent) ?: PLACEHOLDER,
                        sign = row.percentChange,
                        flash = row.flash,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = row.lastPrice?.let { formatPrice(it, row.minStep) } ?: PLACEHOLDER,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        row.change?.let { change ->
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${formatSignedPrice(change, row.minStep)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = signColor(change),
                            )
                        }
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

private const val SHIMMER_ANIMATION_MS = 1100
private const val SHIMMER_SWEEP_WIDTH = 600f

/**
 * A single shared shimmer `Brush`, animated by one [rememberInfiniteTransition]
 * so every pending row's blocks sweep in sync — the standard Compose approach
 * (an infinite-transition-driven gradient, no extra dependency) kept subtle
 * with a low, narrow-alpha-range gradient rather than a bright highlight.
 */
@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SHIMMER_ANIMATION_MS, easing = LinearEasing),
        ),
        label = "shimmerTranslate",
    )
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    return Brush.horizontalGradient(
        colors = listOf(base, highlight, base),
        startX = (translate - 0.5f) * SHIMMER_SWEEP_WIDTH,
        endX = (translate + 0.5f) * SHIMMER_SWEEP_WIDTH,
    )
}

/**
 * [brush] is only ever null when no row in the list is pending, in which case
 * no [QuoteRowItem] renders a [ShimmerBlock] in the first place; the solid
 * fallback only guards against that invariant being violated.
 */
@Composable
private fun ShimmerBlock(brush: Brush?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(brush ?: SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))),
    )
}

/**
 * A security's logo on a neutral monogram tile. The monogram shows only while
 * no real logo is displayed: during loading, on a load failure, and on
 * Tradernet's own "no logo" response (a 1x1 fully transparent GIF, not an HTTP
 * error — see [TradernetUrls.logoUrl]), detected by its 1x1 intrinsic size
 * since it otherwise reports as a successful load. Once a real logo loads, the
 * monogram is removed rather than left underneath, since most brand logos have
 * a transparent background and would otherwise show both at once.
 */
@Composable
private fun SecurityLogo(ticker: String, modifier: Modifier = Modifier) {
    var showPlaceholder by remember(ticker) { mutableStateOf(true) }
    Box(
        modifier = modifier
            .size(LogoSize)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (showPlaceholder) {
            Text(
                text = ticker.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(TradernetUrls.logoUrl(ticker))
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.size(LogoSize),
            contentScale = ContentScale.Fit,
            onState = { state ->
                showPlaceholder = when (state) {
                    is AsyncImagePainter.State.Success -> {
                        val size = state.painter.intrinsicSize
                        size.width <= 1f || size.height <= 1f
                    }
                    else -> true
                }
            },
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

/**
 * Formats a price-scale value (last price or its points delta) per the
 * `min-step-price-formatting` spec: when [minStep] is known, round to its
 * nearest multiple and pad to exactly its own decimal-place count; otherwise
 * fall back to the board's default trailing-zero-stripped display.
 */
private fun formatPrice(value: Double, minStep: Double?): String =
    if (minStep != null && minStep > 0.0) roundToMinStep(value, minStep).toPlainString() else formatValue(value)

private fun formatSignedPrice(value: Double, minStep: Double?): String {
    val sign = if (value > 0) "+" else ""
    return sign + formatPrice(value, minStep)
}

/**
 * Rounds [value] to the nearest multiple of [minStep] and scales the result
 * to [minStep]'s own decimal-place count (its scale after stripping trailing
 * zeros), so e.g. a 0.0001 step always yields 4 decimal places, even if some
 * are zero.
 */
private fun roundToMinStep(value: Double, minStep: Double): BigDecimal {
    val step = BigDecimal.valueOf(minStep)
    val decimalPlaces = step.stripTrailingZeros().scale().coerceAtLeast(0)
    val steps = BigDecimal.valueOf(value).divide(step, 0, RoundingMode.HALF_UP)
    return steps.multiply(step).setScale(decimalPlaces, RoundingMode.HALF_UP)
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
                        exchange = "FIX",
                        name = "Apple Inc.",
                        lastPrice = 333.23,
                        change = -0.87,
                        percentChange = -0.26,
                        minStep = 0.01,
                        flash = FlashDirection.DOWN,
                    ),
                    QuoteRow(
                        ticker = "SP500.IDX",
                        exchange = "FIX",
                        name = "Index S&P 500",
                        lastPrice = 7839.86,
                        change = 16.41,
                        percentChange = 0.21,
                        minStep = 0.01,
                        flash = FlashDirection.UP,
                    ),
                    QuoteRow(
                        ticker = "TGKA",
                        exchange = "MCX",
                        name = "ПАО ТГК-1",
                        lastPrice = 0.004358,
                        change = 0.000006,
                        percentChange = 0.14,
                        minStep = 0.000002,
                    ),
                    QuoteRow(ticker = "AI.US"),
                ),
                boardStatus = BoardStatus.CONTENT,
            ),
            onAction = {},
        )
    }
}

/** Cold start: every fixed-list row seeded but still pending (no quote yet). */
@Preview(showBackground = true)
@Composable
private fun QuotesScreenPendingPreview() {
    MaterialTheme {
        QuotesScreen(
            state = QuotesState(),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuotesScreenErrorPreview() {
    MaterialTheme {
        QuotesScreen(
            state = QuotesState(boardStatus = BoardStatus.ERROR),
            onAction = {},
        )
    }
}
