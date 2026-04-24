package com.whatever.caro.core.crashlytics

import FirebaseCrashlytics.FIRCrashlytics
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey

@OptIn(ExperimentalForeignApi::class)
internal class FirebaseCrashReporter : CrashReporter {
    private val crashlytics: FIRCrashlytics = FIRCrashlytics.crashlytics()

    override fun log(message: String) {
        crashlytics.log(msg = message)
    }

    override fun recordException(throwable: Throwable) {
        crashlytics.recordError(error = throwable.toNSError())
    }

    override fun setUserId(userId: String?) {
        crashlytics.setUserID(userID = userId)
    }

    override fun setCustomKeys(keys: CrashKeys) {
        crashlytics.setCustomKeysAndValues(keysAndValues = keys.toKeysAndValues())
    }

}

private fun CrashKeys.toKeysAndValues(): Map<Any?, Any> {
    return values.mapKeys { (key, _) ->
        key as Any?
    }
}

private fun Throwable.toNSError(): NSError =
    NSError.errorWithDomain(
        domain = "com.whatever.caro",
        code = 0,
        userInfo =
            mapOf(
                NSLocalizedDescriptionKey to (message ?: this::class.simpleName ?: "Kotlin exception"),
                "exception_name" to this::class.simpleName.orEmpty(),
                "exception_message" to message.orEmpty(),
                "exception_string" to toString(),
            ),
    )
