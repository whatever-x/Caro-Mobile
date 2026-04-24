package com.whatever.caro.core.analytics

data class AnalyticsEvent(
    val name: String,
    val parameters: AnalyticsParameters = AnalyticsParameters(),
)

@DslMarker
annotation class AnalyticsDsl

@AnalyticsDsl
class AnalyticsParameters internal constructor() {
    internal val values = mutableMapOf<String, Any>()

    fun put(key: String, value: String) { values[key] = value }
    fun put(key: String, value: Long) { values[key] = value }
    fun put(key: String, value: Double) { values[key] = value }
    fun put(key: String, value: Boolean) { values[key] = value }

}

fun analyticsEvent(name: String, parameters: AnalyticsParameters.() -> Unit = {}): AnalyticsEvent {
    return AnalyticsEvent(
        name = name,
        parameters = AnalyticsParameters().apply(parameters),
    )
}
