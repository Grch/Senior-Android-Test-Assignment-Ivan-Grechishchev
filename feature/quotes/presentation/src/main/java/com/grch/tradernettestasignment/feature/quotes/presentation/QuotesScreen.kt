package com.grch.tradernettestasignment.feature.quotes.presentation

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.grch.tradernettestasignment.feature.quotes.domain.model.FixedTickers
import kotlinx.coroutines.delay

private val PositiveGreen = Color(0xFF1E9E5A)
private val NegativeRed = Color(0xFFD64545)
private const val PLACEHOLDER = "—"
private const val FLASH_DURATION_MS = 300L
private val LogoSize = 20.dp
private const val RECONNECTING_BOARD_ALPHA = 0.4f

/** Test-only semantics flag: true while the percent badge shows its tick highlight. */
internal val PercentHighlighted = SemanticsPropertyKey<Boolean>("PercentHighlighted")
private var SemanticsPropertyReceiver.percentHighlighted by PercentHighlighted

/**
 * Stateful entry to the quotes screen. Holds the [QuotesViewModel], reports the
 * started/stopped lifecycle as an action, observes state, and forwards
 * actions. Hilt supplies the ViewModel and its API/data dependencies.
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
    val onAction by rememberUpdatedState(viewModel::onAction)
    LifecycleStartEffect(Unit) {
        onAction(QuotesAction.OnScreenStarted(true))
        onStopOrDispose {
            if (activity?.isChangingConfigurations != true) {
                onAction(QuotesAction.OnScreenStarted(false))
            }
        }
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
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics {
                                contentDescription = "Reconnecting"
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SecurityLogo(ticker = row.ticker, logoUrl = row.logoUrl)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = row.ticker,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        color = primaryTextColor(),
                    )
                }
                val secondary = listOfNotNull(row.exchange, row.name).joinToString(" | ")
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
                        color = secondaryTextColor(),
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
                        flashTick = row.flashTick,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = row.lastPrice?.let { formatPrice(it, row.minStep) } ?: PLACEHOLDER,
                            style = MaterialTheme.typography.bodySmall,
                            color = primaryTextColor(),
                        )
                        row.change?.let { change ->
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${formatSignedPrice(change, row.minStep)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = primaryTextColor(),
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

/** Lining-digit height as a fraction of the font size (Roboto cap/digit height is about 0.71-0.72 em). */
private const val DIGIT_HEIGHT_EM = 0.72f
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
 * error — see [QuoteRow.logoUrl]), detected by its 1x1 intrinsic size
 * since it otherwise reports as a successful load. Once a real logo loads, the
 * monogram is removed rather than left underneath, since most brand logos have
 * a transparent background and would otherwise show both at once.
 */
@Composable
private fun SecurityLogo(ticker: String, logoUrl: String, modifier: Modifier = Modifier) {
    var showPlaceholder by remember(ticker, logoUrl) { mutableStateOf(true) }
    Box(
        modifier = modifier
            .size(LogoSize)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (showPlaceholder) {
            Text(
                text = ticker.take(1).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(logoUrl)
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
 * The percent-change value, highlighted on a tick: a solid green/red pill with
 * white text for [FLASH_DURATION_MS] after [flashTick] changes, then plain
 * sign-colored text. Only this badge highlights — not the row.
 */
@Composable
private fun PercentBadge(
    text: String,
    sign: Double?,
    flash: FlashDirection,
    flashTick: Int,
    modifier: Modifier = Modifier,
) {
    val firstSeenTick = remember { flashTick } // don't flash a row just because it (re)entered composition
    var highlighted by remember { mutableStateOf(false) }
    // The timer lives in the UI: a new tick changes the key, so Compose cancels the
    // previous highlight and restarts it.
    LaunchedEffect(flashTick) {
        if (flashTick == firstSeenTick) {
            highlighted = false
            return@LaunchedEffect
        }
        highlighted = true
        delay(FLASH_DURATION_MS)
        highlighted = false
    }
    val background = if (!highlighted) {
        Color.Transparent
    } else when (flash) {
        FlashDirection.UP -> PositiveGreen
        FlashDirection.DOWN -> NegativeRed
        FlashDirection.NONE -> Color.Transparent
    }
    val textColor = if (highlighted) Color.White else signColor(sign)
    val textStyle = MaterialTheme.typography.titleMedium
    // Read only while drawing (after layout), so a plain holder avoids recomposition.
    val baseline = remember { FloatArray(1) }
    Text(
        text = text,
        style = textStyle,
        onTextLayout = { layout -> baseline[0] = layout.firstBaseline },
        fontWeight = FontWeight.Normal,
        color = textColor,
        // The flash pill is drawn outset around the text instead of padding it, so the
        // text's right edge stays aligned with the price line below.
        modifier = modifier.semantics {
            // Exposes the highlight state to UI tests; has no visual effect.
            percentHighlighted = highlighted
        }.drawBehind {
            if (background.alpha == 0f) return@drawBehind
            val p = 2.dp.toPx()
            // The pill hugs the glyphs (digits, + - . % never descend below the baseline)
            // rather than the layout box, which includes extra line-height padding.
            val top = baseline[0] - textStyle.fontSize.toPx() * DIGIT_HEIGHT_EM - p
            val bottom = baseline[0] + p
            drawRoundRect(
                color = background,
                topLeft = Offset(-p, top),
                size = Size(size.width + 2 * p, bottom - top),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        },
    )
}

// Dynamic color tints onSurface/onSurfaceVariant, so row text uses fixed neutrals.
@Composable
@ReadOnlyComposable
private fun primaryTextColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)

@Composable
@ReadOnlyComposable
private fun secondaryTextColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF9A9AA0) else Color(0xFF6E6E73)

@Composable
@ReadOnlyComposable
private fun signColor(value: Double?): Color = when {
    value == null -> secondaryTextColor()
    value > 0 -> PositiveGreen
    value < 0 -> NegativeRed
    else -> primaryTextColor()
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
            state = QuotesState(
                rows = FixedTickers.ALL.map {
                    QuoteRow(ticker = it, logoUrl = "https://example.com/logo.png")
                },
            ),
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
            onAction = {},
        )
    }
}
