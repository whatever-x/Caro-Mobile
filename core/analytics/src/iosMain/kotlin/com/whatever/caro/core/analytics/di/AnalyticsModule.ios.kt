package com.whatever.caro.core.analytics.di

import com.whatever.caro.core.analytics.AnalyticsTracker
import com.whatever.caro.core.analytics.FirebaseAnalyticsTracker
import org.koin.core.module.Module
import org.koin.dsl.module

actual val analyticsModule: Module =
    module {
        single<AnalyticsTracker> { FirebaseAnalyticsTracker() }
    }
