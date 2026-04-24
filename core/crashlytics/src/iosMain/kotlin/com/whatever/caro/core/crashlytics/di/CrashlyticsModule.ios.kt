package com.whatever.caro.core.crashlytics.di

import com.whatever.caro.core.crashlytics.CrashReporter
import com.whatever.caro.core.crashlytics.FirebaseCrashReporter
import org.koin.core.module.Module
import org.koin.dsl.module

actual val crashlyticsModule: Module =
    module {
        single<CrashReporter> { FirebaseCrashReporter() }
    }
