package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object IndianNumberFormatter {

    /**
     * Formats a number using the Indian numbering grouping system:
     * e.g. 0 -> "0", 500 -> "500", 1400 -> "1,400", 251904 -> "2,51,904", 10000000 -> "1,00,00,000"
     */
    fun formatIndian(amount: Long): String {
        val isNegative = amount < 0
        val absVal = abs(amount).toString()

        if (absVal.length <= 3) {
            return if (isNegative) "-$absVal" else absVal
        }

        val lastThree = absVal.substring(absVal.length - 3)
        val remaining = absVal.substring(0, absVal.length - 3)

        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count == 2 && i != 0) {
                sb.append(',')
                count = 0
            }
        }

        val formattedRemaining = sb.reverse().toString()
        val result = "$formattedRemaining,$lastThree"
        return if (isNegative) "-$result" else result
    }

    /**
     * Formats as currency with Rupee symbol: "₹ 2,51,904"
     */
    fun formatRupees(amount: Long): String {
        return "₹ ${formatIndian(amount)}"
    }

    /**
     * Safely parse amount from text input, ignoring non-digits
     */
    fun parseAmount(input: String): Long {
        val digitsOnly = input.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return 0L
        return try {
            digitsOnly.toLong()
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }
}
