package com.whatever.caro.core.analytics

data class AnalyticsEvent(
    val name: String,
    val parameters: Map<String, AnalyticsValue> = emptyMap(),
)
