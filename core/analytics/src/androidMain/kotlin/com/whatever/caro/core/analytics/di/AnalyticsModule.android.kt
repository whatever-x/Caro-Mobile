package com.whatever.caro.core.analytics.di

import com.whatever.caro.core.analytics.AnalyticsTracker
import com.whatever.caro.core.analytics.FirebaseAnalyticsTracker
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

actual val analyticsModule =
    module {
        single<FirebaseAnalyticsTracker>() bind AnalyticsTracker::class
    }
