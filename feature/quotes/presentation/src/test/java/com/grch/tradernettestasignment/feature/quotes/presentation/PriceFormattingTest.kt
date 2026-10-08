package com.grch.tradernettestasignment.feature.quotes.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceFormattingTest {

    @Test
    fun `step 0_0001 pads to four decimals`() {
        assertEquals("1.2000", formatPrice(1.2, 0.0001))
        assertEquals("1.2346", formatPrice(1.23456, 0.0001))
    }

    @Test
    fun `step 0_000002 rounds to the nearest multiple with six decimals`() {
        assertEquals("0.004358", formatPrice(0.004358, 0.000002))
        assertEquals("0.004360", formatPrice(0.004359, 0.000002))
        assertEquals("0.000006", formatSignedPrice(0.000006, 0.000002).removePrefix("+"))
    }

    @Test
    fun `step 0_01 pads to two decimals`() {
        assertEquals("333.20", formatPrice(333.2, 0.01))
        assertEquals("333.23", formatPrice(333.231, 0.01))
        assertEquals("7.00", formatPrice(7.0, 0.01))
    }

    @Test
    fun `step 0_5 rounds to halves with one decimal`() {
        assertEquals("10.5", formatPrice(10.3, 0.5))
        assertEquals("10.0", formatPrice(10.2, 0.5))
        assertEquals("11.0", formatPrice(10.75, 0.5))
    }

    @Test
    fun `integer steps yield zero decimals`() {
        assertEquals("100", formatPrice(100.2, 1.0))
        assertEquals("101", formatPrice(100.5, 1.0))
        assertEquals("100", formatPrice(101.0, 5.0))
        assertEquals("105", formatPrice(103.0, 5.0))
        assertEquals("100", roundToMinStep(102.0, 5.0).toPlainString())
    }

    @Test
    fun `rounding is half up`() {
        assertEquals("0.3", roundToMinStep(0.25, 0.1).toPlainString())
        assertEquals("1.5", roundToMinStep(1.25, 0.5).toPlainString())
    }

    @Test
    fun `null zero or negative minStep falls back to stripped display`() {
        assertEquals("1.5", formatPrice(1.50, null))
        assertEquals("1.5", formatPrice(1.50, 0.0))
        assertEquals("1.5", formatPrice(1.50, -0.01))
        assertEquals("100", formatPrice(100.0, null))
    }

    @Test
    fun `signed formatting`() {
        assertEquals("+1.5", formatSigned(1.5))
        assertEquals("0", formatSigned(0.0))
        assertEquals("-1.5", formatSigned(-1.5))
        assertEquals("+0.50", formatSignedPrice(0.5, 0.01))
        assertEquals("0.00", formatSignedPrice(0.0, 0.01))
        assertEquals("-0.87", formatSignedPrice(-0.87, 0.01))
    }

    @Test
    fun `percent formatting`() {
        assertEquals("+0.21%", formatPercent(0.21))
        assertEquals("-0.26%", formatPercent(-0.26))
        assertEquals("0%", formatPercent(0.0))
    }

    @Test
    fun `large and tiny values avoid scientific notation`() {
        assertEquals("12345678900", formatValue(1.23456789E10))
        assertEquals("0.00000001", formatValue(1.0E-8))
        assertEquals("-0.00000001", formatValue(-1.0E-8))
        assertEquals("12345678900.00", formatPrice(1.23456789E10, 0.01))
        assertEquals("0.00000001", formatPrice(1.0E-8, 0.00000001))
    }
}
