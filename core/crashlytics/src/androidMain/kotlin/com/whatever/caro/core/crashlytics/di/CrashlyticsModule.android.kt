package com.whatever.caro.core.crashlytics.di

import com.whatever.caro.core.crashlytics.CrashReporter
import com.whatever.caro.core.crashlytics.FirebaseCrashReporter
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

actual val crashlyticsModule =
    module {
        single<FirebaseCrashReporter>() bind CrashReporter::class
    }
