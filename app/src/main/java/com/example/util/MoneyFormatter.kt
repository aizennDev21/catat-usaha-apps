package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {
    private val indonesianSymbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val formatter = DecimalFormat("#,###", indonesianSymbols)

    fun format(amount: Long, prefix: Boolean = true): String {
        val formattedNumber = formatter.format(amount)
        return if (prefix) "Rp$formattedNumber" else formattedNumber
    }

    fun formatSigned(amount: Long, isIncome: Boolean): String {
        val formattedNumber = formatter.format(amount)
        return if (isIncome) "+Rp$formattedNumber" else "−Rp$formattedNumber"
    }

    fun formatCompact(amount: Long): String {
        val absAmount = kotlin.math.abs(amount)
        val prefix = if (amount < 0) "-Rp" else "Rp"
        return when {
            absAmount >= 1_000_000_000 -> "$prefix%.1f M".format(Locale("id", "ID"), absAmount / 1_000_000_000.0)
            absAmount >= 1_000_000 -> "$prefix%.1f jt".format(Locale("id", "ID"), absAmount / 1_000_000.0)
            absAmount >= 1_000 -> "$prefix%d rb".format(Locale("id", "ID"), absAmount / 1_000)
            else -> "$prefix$absAmount"
        }
    }

    fun parseClean(input: String): Long {
        val digitsOnly = input.filter { it.isDigit() }
        return digitsOnly.toLongOrNull() ?: 0L
    }
}
