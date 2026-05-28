package com.whatever.caro.core.data.di

import com.whatever.caro.core.data.auth.AuthSessionEventBusImpl
import com.whatever.caro.core.data.provider.AuthTokenProviderImpl
import com.whatever.caro.core.model.auth.AuthSessionEventBus
import com.whatever.caro.core.remote.auth.AuthTokenProvider
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val dataModule =
    module {
        single<AuthTokenProviderImpl>() bind AuthTokenProvider::class
        single<AuthSessionEventBusImpl>() bind AuthSessionEventBus::class
    }
