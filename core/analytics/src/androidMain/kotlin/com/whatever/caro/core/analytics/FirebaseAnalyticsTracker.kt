package com.whatever.caro.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

internal class FirebaseAnalyticsTracker(
    context: Context,
) : AnalyticsTracker {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun logEvent(event: AnalyticsEvent) {
        firebaseAnalytics.logEvent(event.name, event.parameters.toBundle())
    }

    override fun setUserId(userId: String?) {
        firebaseAnalytics.setUserId(userId)
    }

    override fun setUserProperty(
        name: String,
        value: String?,
    ) {
        firebaseAnalytics.setUserProperty(name, value)
    }
}

private fun AnalyticsParameters.toBundle(): Bundle =
    Bundle().apply {
        this@toBundle.values.forEach { (key, value) ->
            when (value) {
                is String -> putString(key, value)
                is Boolean -> putBoolean(key, value)
                is Double -> putDouble(key, value)
                is Long -> putLong(key, value)
            }
        }
    }
