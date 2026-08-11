package com.whatever.caro.core.navigator.di

import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcherImpl
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val navigatorModule =
    module {
        single<NavigationDispatcherImpl>() bind NavigationDispatcher::class
    }
