package com.grch.tradernettestassignment.feature.quotes.presentation

import java.math.BigDecimal
import java.math.RoundingMode

internal fun formatPercent(value: Double): String = formatSigned(value) + "%"

internal fun formatSigned(value: Double): String {
    val sign = if (value > 0) "+" else ""
    return sign + formatValue(value)
}

internal fun formatValue(value: Double): String =
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

/** Rounds to [minStep] and pads to its decimal places when known; otherwise uses the default display. */
internal fun formatPrice(value: Double, minStep: Double?): String =
    if (minStep != null && minStep > 0.0) roundToMinStep(value, minStep).toPlainString() else formatValue(value)

internal fun formatSignedPrice(value: Double, minStep: Double?): String {
    val sign = if (value > 0) "+" else ""
    return sign + formatPrice(value, minStep)
}

/** Rounds [value] to a multiple of [minStep] with [minStep]'s own scale (0.0001 always gives 4 places). */
internal fun roundToMinStep(value: Double, minStep: Double): BigDecimal {
    val step = BigDecimal.valueOf(minStep)
    val decimalPlaces = step.stripTrailingZeros().scale().coerceAtLeast(0)
    val steps = BigDecimal.valueOf(value).divide(step, 0, RoundingMode.HALF_UP)
    return steps.multiply(step).setScale(decimalPlaces, RoundingMode.HALF_UP)
}
