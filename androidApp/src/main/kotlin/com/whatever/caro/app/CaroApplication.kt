package com.whatever.caro.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.whatever.caro.composeApp.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext

class CaroApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        initKoin {
            androidContext(this@CaroApplication)
        }

        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }
    }
}
