package com.whatever.caro.core.analytics

import FirebaseAnalytics.FIRAnalytics
import kotlinx.cinterop.ExperimentalForeignApi

internal class FirebaseAnalyticsTracker : AnalyticsTracker {

    @OptIn(ExperimentalForeignApi::class)
    override fun logEvent(event: AnalyticsEvent) {
        FIRAnalytics.logEventWithName(
            name = event.name,
            parameters = event.parameters.values.toMap())
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun setUserId(userId: String?) {
        FIRAnalytics.setUserID(userID = userId)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun setUserProperty(name: String, value: String?) {
        FIRAnalytics.setUserPropertyString(value = value, forName = name)
    }

}