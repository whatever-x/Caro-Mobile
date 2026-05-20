package com.whatever.caro.feature.splash.di

import com.whatever.caro.feature.splash.SplashViewModel
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val splashModule =
    module {
        viewModel<SplashViewModel>()
    }
