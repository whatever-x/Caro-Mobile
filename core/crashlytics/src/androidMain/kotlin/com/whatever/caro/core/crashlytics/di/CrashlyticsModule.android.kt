package com.whatever.caro.core.crashlytics.di

import com.whatever.caro.core.crashlytics.CrashReporter
import com.whatever.caro.core.crashlytics.FirebaseCrashReporter
import org.koin.dsl.module

actual val crashlyticsModule =
    module {
        single<CrashReporter> { FirebaseCrashReporter() }
    }
