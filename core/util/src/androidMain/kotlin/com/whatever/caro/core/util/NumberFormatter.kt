package com.whatever.caro.core.util

import java.text.NumberFormat
import java.util.Locale

actual object NumberFormatter {
    actual fun Int.formatWithComma(): String = NumberFormat.getNumberInstance(Locale.getDefault()).format(this)
}
