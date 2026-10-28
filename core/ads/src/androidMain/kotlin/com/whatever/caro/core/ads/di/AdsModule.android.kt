package com.whatever.caro.core.ads.di

import com.whatever.caro.core.ads.AndroidInterstitialProvider
import com.whatever.caro.core.ads.NativeInterstitialProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

actual val nativeAdsModule =
    module {
        single { AndroidInterstitialProvider(androidContext(), get()) } bind NativeInterstitialProvider::class
    }
