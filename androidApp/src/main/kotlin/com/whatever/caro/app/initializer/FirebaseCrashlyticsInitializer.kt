package com.whatever.caro.app.initializer

import android.content.Context
import androidx.startup.Initializer
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.github.aakira.napier.Napier

class FirebaseCrashlyticsInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true

        Napier.d("Firebase Crashlytics 초기화")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(FirebaseAppInitializer::class.java)

}
