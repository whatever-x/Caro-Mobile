package com.whatever.caro.core.data.di

import com.whatever.caro.core.data.repository.AuthRepository
import com.whatever.caro.core.data.repository.AuthRepositoryImpl
import com.whatever.caro.core.data.repository.FcmTokenRepository
import com.whatever.caro.core.data.repository.FcmTokenRepositoryImpl
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val dataModule =
    module {
        single<FcmTokenRepositoryImpl>() bind FcmTokenRepository::class
        single<AuthRepositoryImpl>() bind AuthRepository::class
    }
