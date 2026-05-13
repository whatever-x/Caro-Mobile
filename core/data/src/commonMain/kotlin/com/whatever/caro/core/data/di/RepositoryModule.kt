package com.whatever.caro.core.data.di

import com.whatever.caro.core.data.auth.AuthTokenProviderImpl
import com.whatever.caro.core.data.repository.AuthRepository
import com.whatever.caro.core.data.repository.AuthRepositoryImpl
import com.whatever.caro.core.data.repository.FcmTokenRepository
import com.whatever.caro.core.data.repository.FcmTokenRepositoryImpl
import com.whatever.caro.core.remote.auth.AuthTokenProvider
import org.koin.dsl.module

val dataModule =
    module {
        single<FcmTokenRepository> { FcmTokenRepositoryImpl() }
        single<AuthRepository> {
            AuthRepositoryImpl(
                remoteAuthDataSource = get(),
                tokenLocalDataSource = get(),
            )
        }
        single<AuthTokenProvider> {
            AuthTokenProviderImpl(
                tokenLocalDataSource = get(),
                tokenRemoteDataSource = get(),
            )
        }
    }
