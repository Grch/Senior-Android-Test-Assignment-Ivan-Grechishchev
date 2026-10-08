package com.grch.tradernettestassignment.feature.quotes.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// Compose Color constants are PascalCase vals by convention.
internal val PositiveGreen = Color(0xFF1E9E5A)
internal val NegativeRed = Color(0xFFD64545)

private val PrimaryTextLight = Color(0xFF1C1C1E)
private val PrimaryTextDark = Color(0xFFF2F2F7)
private val SecondaryTextLight = Color(0xFF6E6E73)
private val SecondaryTextDark = Color(0xFF9A9AA0)

// Dynamic color tints onSurface/onSurfaceVariant, so row text uses fixed neutrals.
@Composable
@ReadOnlyComposable
internal fun primaryTextColor(): Color =
    if (isSystemInDarkTheme()) PrimaryTextDark else PrimaryTextLight

@Composable
@ReadOnlyComposable
internal fun secondaryTextColor(): Color =
    if (isSystemInDarkTheme()) SecondaryTextDark else SecondaryTextLight

@Composable
@ReadOnlyComposable
internal fun signColor(value: Double?): Color = when {
    value == null -> secondaryTextColor()
    value > 0 -> PositiveGreen
    value < 0 -> NegativeRed
    else -> primaryTextColor()
}
