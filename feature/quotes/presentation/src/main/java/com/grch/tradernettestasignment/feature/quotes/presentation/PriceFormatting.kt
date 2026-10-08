package com.grch.tradernettestasignment.feature.quotes.presentation

import java.math.BigDecimal
import java.math.RoundingMode

internal fun formatPercent(value: Double): String = formatSigned(value) + "%"

internal fun formatSigned(value: Double): String {
    val sign = if (value > 0) "+" else ""
    return sign + formatValue(value)
}

internal fun formatValue(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

/**
 * Formats a price-scale value (last price or its points delta) per the
 * `min-step-price-formatting` spec: when [minStep] is known, round to its
 * nearest multiple and pad to exactly its own decimal-place count; otherwise
 * fall back to the board's default trailing-zero-stripped display.
 */
internal fun formatPrice(value: Double, minStep: Double?): String =
    if (minStep != null && minStep > 0.0) roundToMinStep(value, minStep).toPlainString() else formatValue(value)

internal fun formatSignedPrice(value: Double, minStep: Double?): String {
    val sign = if (value > 0) "+" else ""
    return sign + formatPrice(value, minStep)
}

/**
 * Rounds [value] to the nearest multiple of [minStep] and scales the result
 * to [minStep]'s own decimal-place count (its scale after stripping trailing
 * zeros), so e.g. a 0.0001 step always yields 4 decimal places, even if some
 * are zero.
 */
internal fun roundToMinStep(value: Double, minStep: Double): BigDecimal {
    val step = BigDecimal.valueOf(minStep)
    val decimalPlaces = step.stripTrailingZeros().scale().coerceAtLeast(0)
    val steps = BigDecimal.valueOf(value).divide(step, 0, RoundingMode.HALF_UP)
    return steps.multiply(step).setScale(decimalPlaces, RoundingMode.HALF_UP)
}
