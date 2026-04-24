package com.whatever.caro.core.analytics.di

import com.whatever.caro.core.analytics.AnalyticsTracker
import com.whatever.caro.core.analytics.FirebaseAnalyticsTracker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val analyticsModule =
    module {
        single<AnalyticsTracker> {
            FirebaseAnalyticsTracker(context = androidContext())
        }
    }
