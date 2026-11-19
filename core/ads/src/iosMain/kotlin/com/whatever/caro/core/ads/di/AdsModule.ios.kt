package com.whatever.caro.core.ads.di

import com.whatever.caro.core.ads.IosInterstitialProvider
import com.whatever.caro.core.ads.NativeInterstitialProvider
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

actual val nativeAdsModule =
    module {
        single<IosInterstitialProvider>() bind NativeInterstitialProvider::class
    }
