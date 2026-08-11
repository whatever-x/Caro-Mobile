package com.whatever.caro.core.analytics

import FirebaseAnalytics.FIRAnalytics
import FirebaseCore.FIRApp
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
fun configureFirebaseApp() {
    if (FIRApp.defaultApp() == null) {
        FIRApp.configure()
    }
}

@OptIn(ExperimentalForeignApi::class)
fun setAnalyticsCollectionEnabled(enabled: Boolean) {
    FIRAnalytics.setAnalyticsCollectionEnabled(enabled)
}
