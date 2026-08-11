package com.guidovezzoni.fakestore.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ProductListFormatterTest {

    @Test
    fun `GIVEN a price of 109_95 WHEN formatPrice is called with Locale_US THEN it returns dollar sign 109_95`() {
        val expected = "$109.95"

        val result = formatPrice(109.95, Locale.US)

        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN a price of 109_95 WHEN formatPrice is called with Locale es_ES THEN it returns USD denominated string with Spanish conventions`() {
        val result = formatPrice(109.95, Locale("es", "ES"))

        assert(!result.contains("€")) { "Expected no Euro symbol but got: $result" }
        assert(result.contains(",")) { "Expected comma as decimal separator but got: $result" }
        assert(result.contains("US") || result.contains("USD")) {
            "Expected USD currency indicator but got: $result"
        }
    }

    @Test
    fun `GIVEN a rating score of 4_1 WHEN formatRatingScore is called with Locale_US THEN it returns 4_1`() {
        val expected = "4.1"

        val result = formatRatingScore(4.1, Locale.US)

        assertEquals(expected, result)
    }

    @Test
    fun `GIVEN a rating score of 4_1 WHEN formatRatingScore is called with Locale es_ES THEN it returns 4 comma 1`() {
        val expected = "4,1"

        val result = formatRatingScore(4.1, Locale("es", "ES"))

        assertEquals(expected, result)
    }
}
