package com.example.openmeteoweather.domain.util

import java.util.Locale

fun Double.formatDecimal(): String {
    val s = String.format(Locale.US, "%.2f", this)
    return if (s.contains(".")) {
        var str = s
        while (str.endsWith("0")) {
            str = str.substring(0, str.length - 1)
        }
        if (str.endsWith(".")) {
            str = str.substring(0, str.length - 1)
        }
        str
    } else {
        s
    }
}

fun Double?.formatDecimal(fallback: String = "N/A"): String {
    return this?.formatDecimal() ?: fallback
}
