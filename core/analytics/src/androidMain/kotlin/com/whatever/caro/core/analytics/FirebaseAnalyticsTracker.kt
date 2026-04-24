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

private fun Map<String, AnalyticsValue>.toBundle(): Bundle =
    Bundle().apply {
        forEach { (key, value) ->
            when (value) {
                is AnalyticsValue.BooleanValue -> putBoolean(key, value.value)
                is AnalyticsValue.DoubleValue -> putDouble(key, value.value)
                is AnalyticsValue.LongValue -> putLong(key, value.value)
                is AnalyticsValue.StringValue -> putString(key, value.value)
            }
        }
    }
