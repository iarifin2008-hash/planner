package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {

    fun formatRupiah(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        return "Rp" + formatter.format(amount)
    }

    fun formatRupiahShort(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> String.format(Locale.US, "Rp%.1fM", amount / 1_000_000_000.0)
            amount >= 1_000_000 -> String.format(Locale.US, "Rp%.1fJt", amount / 1_000_000.0)
            amount >= 1_000 -> String.format(Locale.US, "Rp%.0fRb", amount / 1_000.0)
            else -> formatRupiah(amount)
        }
    }

    fun parseRupiah(input: String): Double {
        val clean = input.replace("Rp", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()
        return clean.toDoubleOrNull() ?: 0.0
    }

    fun formatPercent(percent: Double): String {
        return String.format(Locale.US, "%.1f%%", percent)
    }
}
