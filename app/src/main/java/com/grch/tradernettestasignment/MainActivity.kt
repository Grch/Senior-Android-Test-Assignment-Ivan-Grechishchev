package com.grch.tradernettestasignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.grch.tradernettestasignment.feature.quotes.api.QuotesEntryPoint
import com.grch.tradernettestasignment.feature.quotes.impl.DefaultQuotesEntryPoint
import com.grch.tradernettestasignment.ui.theme.TradernetTestAsignmentTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TradernetTestAsignmentTheme {
                AppRoot()
            }
        }
    }
}

/**
 * Composition root: the only place that binds the concrete quotes implementation
 * from `:feature:quotes:impl` to the public `:feature:quotes:api` entry contract.
 *
 * The app hosts the quotes destination directly; the foundation deliberately
 * does not add a navigation framework.
 */
@Composable
private fun AppRoot(modifier: Modifier = Modifier) {
    val quotesEntryPoint: QuotesEntryPoint = remember { DefaultQuotesEntryPoint() }
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        quotesEntryPoint.Content(modifier = Modifier.padding(innerPadding))
    }
}
