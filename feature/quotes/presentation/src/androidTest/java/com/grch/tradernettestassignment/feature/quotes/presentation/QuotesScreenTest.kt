package com.grch.tradernettestassignment.feature.quotes.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuotesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun string(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val row = QuoteRow(
        ticker = "AAPL.US",
        logoUrl = "",
        exchange = "FIX",
        name = "Apple Inc.",
        lastPrice = 333.2,
        change = -0.87,
        percentChange = -0.26,
        minStep = 0.01,
    )

    private fun highlighted(expected: Boolean) =
        SemanticsMatcher.expectValue(PercentHighlighted, expected)

    @Test
    fun contentRendersRowTexts() {
        composeRule.setContent {
            QuotesScreen(state = QuotesState(rows = listOf(row)), onRetryClick = {})
        }

        composeRule.onNodeWithText("AAPL.US").assertIsDisplayed()
        composeRule.onNodeWithText("FIX | Apple Inc.").assertIsDisplayed()
        composeRule.onNodeWithText("333.20").assertIsDisplayed()
        composeRule.onNodeWithText("(-0.87)").assertIsDisplayed()
        composeRule.onNodeWithText("-0.26%").assertIsDisplayed()
    }

    @Test
    fun pendingRowShowsNoPrice() {
        composeRule.setContent {
            QuotesScreen(
                state = QuotesState(rows = listOf(QuoteRow(ticker = "AI.US", logoUrl = ""))),
                onRetryClick = {},
            )
        }

        composeRule.onNodeWithText("AI.US").assertIsDisplayed()
        composeRule.onAllNodesWithText("—").assertCountEquals(0)
        composeRule.onAllNodesWithText("%", substring = true).assertCountEquals(0)
    }

    @Test
    fun errorShowsTextAndRetryInvokesCallback() {
        var retryClicks = 0
        composeRule.setContent {
            QuotesScreen(
                state = QuotesState(boardStatus = BoardStatus.ERROR),
                onRetryClick = { retryClicks++ },
            )
        }

        composeRule.onNodeWithText(string(R.string.quotes_error_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.quotes_error_retry)).performClick()

        assertEquals(1, retryClicks)
    }

    private val progressIndicator =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)

    @Test
    fun reconnectingShowsRowsAndSpinnerButNoRetry() {
        composeRule.setContent {
            QuotesScreen(
                state = QuotesState(rows = listOf(row), boardStatus = BoardStatus.RECONNECTING),
                onRetryClick = {},
            )
        }

        // Rows are hidden from semantics while reconnecting (screen readers skip the dimmed board).
        composeRule.onAllNodesWithText("AAPL.US").assertCountEquals(0)
        composeRule.onNode(progressIndicator).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.quotes_reconnecting)).assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.quotes_error_retry)).assertCountEquals(0)
    }

    @Test
    fun reconnectingToContentKeepsRowsAndScrollPosition() {
        val rows = (1..60).map { row.copy(ticker = "T$it.US") }
        var status by mutableStateOf(BoardStatus.CONTENT)
        composeRule.setContent {
            QuotesScreen(state = QuotesState(rows = rows, boardStatus = status), onRetryClick = {})
        }
        composeRule.onNode(hasScrollAction()).performScrollToIndex(30)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("T31.US").assertIsDisplayed()

        status = BoardStatus.RECONNECTING
        composeRule.waitForIdle()
        composeRule.onNode(progressIndicator).assertIsDisplayed()

        status = BoardStatus.CONTENT
        composeRule.waitForIdle()
        composeRule.onAllNodes(progressIndicator).assertCountEquals(0)
        composeRule.onNodeWithText("T31.US").assertIsDisplayed()
        composeRule.onAllNodesWithText("T1.US").assertCountEquals(0)
    }

    @Test
    fun contentShowsNoSpinner() {
        composeRule.setContent {
            QuotesScreen(state = QuotesState(rows = listOf(row)), onRetryClick = {})
        }

        composeRule.onAllNodes(progressIndicator).assertCountEquals(0)
    }

    @Test
    fun highlightFollowsFlashTickAndExpiresAfter300ms() {
        composeRule.mainClock.autoAdvance = false
        var current by mutableStateOf(row.copy(flash = FlashDirection.UP, flashTick = 5))
        composeRule.setContent {
            QuotesScreen(state = QuotesState(rows = listOf(current)), onRetryClick = {})
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("-0.26%").assert(highlighted(false))

        current = current.copy(flashTick = 6)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("-0.26%").assert(highlighted(true))

        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithText("-0.26%").assert(highlighted(true))

        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithText("-0.26%").assert(highlighted(false))
    }

    @Test
    fun secondTickBeforeExpiryExtendsHighlight() {
        composeRule.mainClock.autoAdvance = false
        var current by mutableStateOf(row.copy(flash = FlashDirection.UP, flashTick = 1))
        composeRule.setContent {
            QuotesScreen(state = QuotesState(rows = listOf(current)), onRetryClick = {})
        }
        composeRule.mainClock.advanceTimeByFrame()

        current = current.copy(flashTick = 2)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithText("-0.26%").assert(highlighted(true))

        current = current.copy(flashTick = 3)
        composeRule.mainClock.advanceTimeByFrame()
        // 200ms past the second tick: would have expired without the restart.
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithText("-0.26%").assert(highlighted(true))

        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithText("-0.26%").assert(highlighted(false))
    }
}
