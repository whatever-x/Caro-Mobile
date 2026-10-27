package com.whatever.caro.core.ads.di

import com.whatever.caro.core.ads.DefaultInterstitialAdController
import com.whatever.caro.core.ads.InterstitialAdController
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

expect val nativeAdsModule: Module

val adsModule =
    module {
        includes(nativeAdsModule)
        single { DefaultInterstitialAdController(get(), get()) } bind InterstitialAdController::class
    }
