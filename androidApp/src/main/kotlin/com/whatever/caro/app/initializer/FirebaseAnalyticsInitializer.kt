package com.whatever.caro.app.initializer

import android.content.Context
import androidx.startup.Initializer
import com.google.firebase.analytics.FirebaseAnalytics
import com.whatever.caro.app.BuildConfig
import io.github.aakira.napier.Napier

class FirebaseAnalyticsInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)

        Napier.d("Firebase Analytics 초기화")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(FirebaseAppInitializer::class.java)
}
