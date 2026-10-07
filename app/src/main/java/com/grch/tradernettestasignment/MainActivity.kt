package com.grch.tradernettestasignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.grch.tradernettestasignment.feature.quotes.impl.QuotesRoot
import com.grch.tradernettestasignment.ui.theme.TradernetTestAsignmentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TradernetTestAsignmentTheme {
                QuotesRoot()
            }
        }
    }
}
