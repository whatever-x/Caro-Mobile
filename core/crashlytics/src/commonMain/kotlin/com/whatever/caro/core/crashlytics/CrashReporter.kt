package com.whatever.caro.core.crashlytics

interface CrashReporter {
    fun log(message: String)

    fun recordException(throwable: Throwable)

    fun setUserId(userId: String?)

    fun setCustomKeys(keys: CrashKeys)
}
