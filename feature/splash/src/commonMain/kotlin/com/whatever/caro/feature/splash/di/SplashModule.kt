package com.whatever.caro.feature.splash.di

import com.whatever.caro.feature.splash.SplashViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val splashModule =
    module {
        viewModel<SplashViewModel> { SplashViewModel(get(), get()) }
    }
