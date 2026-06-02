package com.whatever.caro.core.util

expect object NumberFormatter {
    fun Int.formatWithComma(): String
}
