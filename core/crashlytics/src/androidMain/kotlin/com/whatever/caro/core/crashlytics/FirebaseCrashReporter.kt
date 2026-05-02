package com.whatever.caro.core.crashlytics

import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics

internal class FirebaseCrashReporter : CrashReporter {
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

    override fun setCustomKeys(keys: CrashKeys) {
        crashlytics.setCustomKeys(keys.toCustomKeysAndValues())
    }
}

private fun CrashKeys.toCustomKeysAndValues(): CustomKeysAndValues =
    CustomKeysAndValues
        .Builder()
        .apply {
            values.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Double -> putDouble(key, value)
                    is Long -> putLong(key, value)
                    is String -> putString(key, value)
                }
            }
        }.build()
