package com.whatever.caro

import android.app.Application
import com.whatever.caro.composeApp.di.initKoin
import org.koin.android.ext.koin.androidContext

class CaroApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@CaroApplication)
        }
    }
}
