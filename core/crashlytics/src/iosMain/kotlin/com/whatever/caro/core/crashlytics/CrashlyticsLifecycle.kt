package com.whatever.caro.core.crashlytics

import FirebaseCrashlytics.FIRCrashlytics
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
    FIRCrashlytics.crashlytics().setCrashlyticsCollectionEnabled(enabled)
}
