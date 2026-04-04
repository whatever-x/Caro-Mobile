package com.whatever.caro.core.navigator.di

import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcherImpl
import org.koin.dsl.module

val navigatorModule =
    module {
        single<NavigationDispatcher> { NavigationDispatcherImpl() }
    }
