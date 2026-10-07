package com.grch.tradernettestasignment.feature.quotes.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Stateful entry to the quotes screen. Holds the [QuotesViewModel], observes its
 * state, and forwards user actions back to it.
 */
@Composable
fun QuotesRoot(
    modifier: Modifier = Modifier,
    viewModel: QuotesViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    QuotesScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = state.placeholderMessage,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { onAction(QuotesAction.OnPlaceholderAction) }) {
            Text(
                text = if (state.isActionDispatched) {
                    "Action dispatched"
                } else {
                    "Dispatch placeholder action"
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QuotesScreenPreview() {
    MaterialTheme {
        QuotesScreen(state = QuotesState(), onAction = {})
    }
}
