package com.whatever.caro.core.data.di

import com.whatever.caro.core.data.auth.AuthSessionEventBusImpl
import com.whatever.caro.core.data.provider.AuthTokenProviderImpl
import com.whatever.caro.core.data.repository.AuthRepository
import com.whatever.caro.core.data.repository.AuthRepositoryImpl
import com.whatever.caro.core.data.repository.FcmTokenRepository
import com.whatever.caro.core.data.repository.FcmTokenRepositoryImpl
import com.whatever.caro.core.model.auth.AuthSessionEventBus
import com.whatever.caro.core.model.auth.AuthSessionEventPublisher
import com.whatever.caro.core.remote.auth.AuthTokenProvider
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val dataModule =
    module {
        single<FcmTokenRepositoryImpl>() bind FcmTokenRepository::class
        single<AuthRepositoryImpl>() bind AuthRepository::class
        single<AuthTokenProviderImpl>() bind AuthTokenProvider::class
        single<AuthSessionEventBusImpl>() binds arrayOf(AuthSessionEventBus::class, AuthSessionEventPublisher::class)
    }
