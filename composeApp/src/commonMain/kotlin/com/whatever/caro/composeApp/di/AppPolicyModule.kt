package com.whatever.caro.composeApp.di

import com.whatever.caro.core.viewmodel.ExceptionFilter
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val appPolicyModule =
    module {
        single<CaroExceptionFilter>() bind ExceptionFilter::class
    }
