package com.whatever.caro.core.crashlytics

import com.google.firebase.crashlytics.FirebaseCrashlytics

internal class FirebaseCrashReporterImpl : CrashReporter {
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun setUserId(userId: String?) {
        crashlytics.setUserId(userId.orEmpty())
    }

    override fun setCustomKey(
        key: String,
        value: CrashValue,
    ) {
        when (value) {
            is CrashValue.BooleanValue -> crashlytics.setCustomKey(key, value.value)
            is CrashValue.DoubleValue -> crashlytics.setCustomKey(key, value.value)
            is CrashValue.LongValue -> crashlytics.setCustomKey(key, value.value)
            is CrashValue.StringValue -> crashlytics.setCustomKey(key, value.value)
        }
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        crashlytics.isCrashlyticsCollectionEnabled = enabled
    }
}
