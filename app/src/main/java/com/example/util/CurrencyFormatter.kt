package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {

    fun format(amount: Double, symbol: String = "₹"): String {
        val negative = amount < -0.004
        val absVal = abs(amount)
        val isWhole = abs(absVal - absVal.roundToLong().toDouble()) < 0.005
        val formattedNumber = formatIndianNumber(absVal, includeDecimals = !isWhole)
        return if (negative) "-$symbol$formattedNumber" else "$symbol$formattedNumber"
    }

    fun formatIndianNumber(value: Double, includeDecimals: Boolean = false): String {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH)
        val rounded = if (includeDecimals) {
            String.format(Locale.ENGLISH, "%.2f", value)
        } else {
            value.roundToLong().toString()
        }
        val parts = rounded.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) ".${parts[1]}" else ""

        if (intPart.length <= 3) return intPart + decPart

        val lastThree = intPart.takeLast(3)
        val remaining = intPart.dropLast(3)
        val grouped = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            grouped.append(remaining[i])
            count++
            if (count == 2 && i > 0) {
                grouped.append(',')
                count = 0
            }
        }
        val prefix = grouped.reverse().toString()
        val _unused = DecimalFormat("#,##0", symbols) // keeps import clean if extended
        return "$prefix,$lastThree$decPart"
    }
}
