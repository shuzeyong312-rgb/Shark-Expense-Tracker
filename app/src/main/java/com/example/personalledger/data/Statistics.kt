package com.example.personalledger.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek
import java.math.BigInteger

enum class PeriodKind { WEEK, MONTH, YEAR }
data class Period(val start: LocalDate, val end: LocalDate, val kind: PeriodKind) {
    fun slots(): List<LocalDate> = when (kind) {
        PeriodKind.WEEK, PeriodKind.MONTH -> (0..java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()).map { start.plusDays(it.toLong()) }
        PeriodKind.YEAR -> (0..11).map { start.plusMonths(it.toLong()) }
    }
}
fun periodFor(anchor: LocalDate, kind: PeriodKind): Period = when (kind) {
    PeriodKind.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).let { Period(it, it.plusDays(6), kind) }
    PeriodKind.MONTH -> YearMonth.from(anchor).let { Period(it.atDay(1), it.atEndOfMonth(), kind) }
    PeriodKind.YEAR -> Period(anchor.withDayOfYear(1), anchor.withMonth(12).withDayOfMonth(31), kind)
}
fun periodSeries(transactions: List<LedgerTransaction>, period: Period, type: String): List<BigInteger> {
    val totals = transactions.asSequence().filter { it.type == type }.map { LocalDate.parse(it.occurredOn) to it.amountMinor }
        .filter { (date, _) -> date >= period.start && date <= period.end }
        .groupingBy { (date, _) -> if (period.kind == PeriodKind.YEAR) date.withDayOfMonth(1) else date }
        .fold(BigInteger.ZERO) { sum, item -> sum + BigInteger.valueOf(item.second) }
    return period.slots().map { totals[it] ?: BigInteger.ZERO }
}
