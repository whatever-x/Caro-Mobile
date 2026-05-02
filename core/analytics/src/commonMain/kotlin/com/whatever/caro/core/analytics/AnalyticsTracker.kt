package com.whatever.caro.core.analytics

interface AnalyticsTracker {
    fun logEvent(event: AnalyticsEvent)

    fun setUserId(userId: String?)

    fun setUserProperty(
        name: String,
        value: String?,
    )
}
