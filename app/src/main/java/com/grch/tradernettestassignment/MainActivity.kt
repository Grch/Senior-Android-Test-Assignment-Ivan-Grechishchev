package com.grch.tradernettestassignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.grch.tradernettestassignment.feature.quotes.presentation.QuotesRoot
import com.grch.tradernettestassignment.ui.theme.TradernetTestAssignmentTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TradernetTestAssignmentTheme {
                QuotesRoot()
            }
        }
    }
}
