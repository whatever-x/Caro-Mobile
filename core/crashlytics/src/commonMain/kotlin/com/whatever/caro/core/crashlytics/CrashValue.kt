package com.whatever.caro.core.crashlytics

sealed interface CrashValue {
    data class StringValue(
        val value: String,
    ) : CrashValue

    data class LongValue(
        val value: Long,
    ) : CrashValue

    data class DoubleValue(
        val value: Double,
    ) : CrashValue

    data class BooleanValue(
        val value: Boolean,
    ) : CrashValue
}

fun String.asCrashValue(): CrashValue = CrashValue.StringValue(this)

fun Long.asCrashValue(): CrashValue = CrashValue.LongValue(this)

fun Double.asCrashValue(): CrashValue = CrashValue.DoubleValue(this)

fun Boolean.asCrashValue(): CrashValue = CrashValue.BooleanValue(this)
