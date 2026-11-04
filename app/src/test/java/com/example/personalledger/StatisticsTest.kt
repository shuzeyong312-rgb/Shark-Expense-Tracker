package com.example.personalledger

import com.example.personalledger.data.*
import java.time.LocalDate
import java.math.BigInteger
import org.junit.Assert.*
import org.junit.Test

class StatisticsTest {
    @Test fun periodsUseCalendarBoundaries() {
        val leap = periodFor(LocalDate.of(2024, 2, 20), PeriodKind.MONTH)
        assertEquals(29, leap.slots().size)
        val week = periodFor(LocalDate.of(2026, 1, 1), PeriodKind.WEEK)
        assertEquals(LocalDate.of(2025, 12, 29), week.start)
        assertEquals(LocalDate.of(2026, 1, 4), week.end)
        assertEquals(12, periodFor(LocalDate.now(), PeriodKind.YEAR).slots().size)
        listOf(YearMonthCase(2023, 2, 28), YearMonthCase(2024, 2, 29), YearMonthCase(2026, 4, 30), YearMonthCase(2026, 1, 31)).forEach {
            assertEquals(it.days, periodFor(LocalDate.of(it.year, it.month, 1), PeriodKind.MONTH).slots().size)
        }
    }
    @Test fun seriesFiltersTypeAndIncludesZeroDays() {
        val period = periodFor(LocalDate.of(2026, 1, 1), PeriodKind.WEEK)
        val items = listOf(LedgerTransaction(1, "EXPENSE", "food", null, null, 123, "2026-01-01", "", ""), LedgerTransaction(2, "INCOME", "salary", null, null, 999, "2026-01-01", "", ""))
        assertEquals(listOf(0L, 0L, 0L, 123L, 0L, 0L, 0L).map(BigInteger::valueOf), periodSeries(items, period, "EXPENSE"))
    }
    @Test fun totalsDoNotOverflowLong() {
        val period = periodFor(LocalDate.of(2026, 1, 1), PeriodKind.MONTH)
        val row = LedgerTransaction(1, "EXPENSE", "food", null, null, Long.MAX_VALUE, "2026-01-01", "", "")
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO), periodSeries(listOf(row, row.copy(id = 2)), period, "EXPENSE").sumBig())
    }
    private data class YearMonthCase(val year: Int, val month: Int, val days: Int)
}
