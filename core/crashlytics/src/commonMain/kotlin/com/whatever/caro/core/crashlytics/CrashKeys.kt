package com.whatever.caro.core.crashlytics

data class CrashKeys(
    val values: Map<String, Any>,
)

@DslMarker
annotation class CrashlyticsDsl

@CrashlyticsDsl
class CrashKeyValues internal constructor() {
    internal val values = mutableMapOf<String, Any>()

    fun put(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    fun put(
        key: String,
        value: Long,
    ) {
        values[key] = value
    }

    fun put(
        key: String,
        value: Double,
    ) {
        values[key] = value
    }

    fun put(
        key: String,
        value: Boolean,
    ) {
        values[key] = value
    }
}

fun crashKeys(values: CrashKeyValues.() -> Unit): CrashKeys =
    CrashKeys(
        values = CrashKeyValues().apply(values).values,
    )
