package com.whatever.caro.core.util

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

actual object NumberFormatter {
    actual fun Int.formatWithComma(): String {
        val formatter =
            NSNumberFormatter().apply {
                numberStyle = NSNumberFormatterDecimalStyle
            }
        return formatter.stringFromNumber(NSNumber(this)) ?: this.toString()
    }
}
