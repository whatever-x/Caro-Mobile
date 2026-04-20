package com.whatever.caro.core.analytics

sealed interface AnalyticsValue {
    data class StringValue(
        val value: String,
    ) : AnalyticsValue

    data class LongValue(
        val value: Long,
    ) : AnalyticsValue

    data class DoubleValue(
        val value: Double,
    ) : AnalyticsValue

    data class BooleanValue(
        val value: Boolean,
    ) : AnalyticsValue
}
