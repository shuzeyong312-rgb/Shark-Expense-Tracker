package com.example.personalledger

import com.example.personalledger.data.formatAmountMinor
import com.example.personalledger.data.parseAmountMinor
import com.example.personalledger.data.calculateAmountMinor
import com.example.personalledger.data.formatAverageMinor
import com.example.personalledger.data.formatPercent
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    @Test fun calculatesExactCentsAndRejectsInvalidResults() {
        assertEquals(102L, calculateAmountMinor("1.01+0.02-0.01"))
        assertEquals(100L, calculateAmountMinor("0-1+2"))
        assertEquals(0L, calculateAmountMinor("1-1"))
        assertEquals(-100L, calculateAmountMinor("1-2"))
        assertEquals(100L, calculateAmountMinor("-1+2"))
        listOf("", "1+", "1.", "1+-2", "1.234", "92233720368547758.08").forEach { assertNull(calculateAmountMinor(it)) }
    }
    @Test fun acceptsExactPositiveCents() {
        assertEquals(1234L, parseAmountMinor("12.34"))
        assertEquals(100L, parseAmountMinor("1"))
        assertEquals("0.01", formatAmountMinor(1))
    }
    @Test fun rejectsInvalidAndOverflow() {
        listOf("", "0", "-1", "1.234", "1.", "1e2", "99999999999999999999999").forEach { assertNull(parseAmountMinor(it)) }
    }
    @Test fun averageAndPercentageRoundWithoutOverflow() {
        assertEquals("0.01", formatAverageMinor(1, 2))
        assertEquals("0.00", formatAverageMinor(0, 0))
        assertEquals("100.0", formatPercent(Long.MAX_VALUE, Long.MAX_VALUE))
    }
}
