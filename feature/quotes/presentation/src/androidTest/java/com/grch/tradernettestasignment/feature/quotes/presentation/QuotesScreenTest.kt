package com.grch.tradernettestasignment.feature.quotes.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class QuotesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

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
            QuotesScreen(state = QuotesState(rows = listOf(row)), onAction = {})
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
                onAction = {},
            )
        }

        composeRule.onNodeWithText("AI.US").assertIsDisplayed()
        composeRule.onAllNodesWithText("—").assertCountEquals(0)
        composeRule.onAllNodesWithText("%", substring = true).assertCountEquals(0)
    }

    @Test
    fun errorShowsTextAndRetryDispatchesAction() {
        val actions = mutableListOf<QuotesAction>()
        composeRule.setContent {
            QuotesScreen(
                state = QuotesState(boardStatus = BoardStatus.ERROR),
                onAction = { actions += it },
            )
        }

        composeRule.onNodeWithText("Oops, seems we've got an error").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").performClick()

        assertEquals(listOf<QuotesAction>(QuotesAction.OnRetryClicked), actions)
    }

    @Test
    fun highlightFollowsFlashTickAndExpiresAfter300ms() {
        composeRule.mainClock.autoAdvance = false
        var current by mutableStateOf(row.copy(flash = FlashDirection.UP, flashTick = 5))
        composeRule.setContent {
            QuotesScreen(state = QuotesState(rows = listOf(current)), onAction = {})
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
            QuotesScreen(state = QuotesState(rows = listOf(current)), onAction = {})
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
