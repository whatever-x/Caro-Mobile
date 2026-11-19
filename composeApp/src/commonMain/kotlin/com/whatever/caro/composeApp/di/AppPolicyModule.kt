package com.whatever.caro.composeApp.di

import com.whatever.caro.composeApp.AppStartupViewModel
import com.whatever.caro.composeApp.policy.CaroExceptionFilter
import com.whatever.caro.core.data.repository.ads.AdEligibilityMonitor
import com.whatever.caro.core.viewmodel.ExceptionFilter
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val appPolicyModule =
    module {
        single<CaroExceptionFilter>() bind ExceptionFilter::class
        viewModel { AppStartupViewModel(get(), get(), get<AdEligibilityMonitor>().state, get<AdEligibilityMonitor>()::recheck) }
    }
