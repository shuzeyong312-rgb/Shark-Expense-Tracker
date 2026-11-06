package com.example.personalledger.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.math.BigInteger

fun parseAmountMinor(input: String): Long? {
    if (!input.matches(Regex("[0-9]+(\\.[0-9]{1,2})?"))) return null
    return try {
        BigDecimal(input).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact().takeIf { it > 0 }
    } catch (_: ArithmeticException) { null }
}

fun calculateAmountMinor(input: String): Long? {
    if (!input.matches(Regex("-?[0-9]+(\\.[0-9]{1,2})?([+-][0-9]+(\\.[0-9]{1,2})?)*"))) return null
    return try {
        val terms = Regex("[+-]?[0-9]+(?:\\.[0-9]{1,2})?").findAll(input)
        terms.fold(BigDecimal.ZERO) { total, term -> total + BigDecimal(term.value) }
            .movePointRight(2).longValueExact()
    } catch (_: ArithmeticException) { null }
}

fun formatAmountMinor(amount: Long): String = BigDecimal.valueOf(amount, 2).toPlainString()
fun formatAverageMinor(amount: Long, count: Int): String = if (count == 0) "0.00" else BigDecimal.valueOf(amount, 2).divide(BigDecimal.valueOf(count.toLong()), 2, RoundingMode.HALF_UP).toPlainString()
fun formatPercent(amount: Long, total: Long): String = if (total == 0L) "0.0" else BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).toPlainString()
fun formatAmountMinor(amount: BigInteger): String = BigDecimal(amount, 2).toPlainString()
fun formatAverageMinor(amount: BigInteger, count: Int): String = if (count == 0) "0.00" else BigDecimal(amount, 2).divide(BigDecimal.valueOf(count.toLong()), 2, RoundingMode.HALF_UP).toPlainString()
fun formatPercent(amount: BigInteger, total: BigInteger): String = if (total.signum() == 0) "0.0" else BigDecimal(amount).multiply(BigDecimal.valueOf(100)).divide(BigDecimal(total), 1, RoundingMode.HALF_UP).toPlainString()
fun Iterable<LedgerTransaction>.sumMinor(): BigInteger = fold(BigInteger.ZERO) { sum, item -> sum + BigInteger.valueOf(item.amountMinor) }
fun Iterable<BigInteger>.sumBig(): BigInteger = fold(BigInteger.ZERO, BigInteger::add)
