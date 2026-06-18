package com.whatever.caro.core.data.di

import com.whatever.caro.core.data.provider.AuthTokenProviderImpl
import com.whatever.caro.core.data.repository.AuthRepository
import com.whatever.caro.core.data.repository.AuthRepositoryImpl
import com.whatever.caro.core.data.repository.FcmTokenRepository
import com.whatever.caro.core.data.repository.FcmTokenRepositoryImpl
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.data.repository.deck.DeckRepositoryImpl
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepositoryImpl
import com.whatever.caro.core.remote.auth.AuthTokenProvider
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val repositoryModule =
    module {
        single<FcmTokenRepositoryImpl>() bind FcmTokenRepository::class
        single<AuthRepositoryImpl>() bind AuthRepository::class
        single<AuthTokenProviderImpl>() bind AuthTokenProvider::class
        single<ProfileRepositoryImpl>() bind ProfileRepository::class
        single<DeckRepositoryImpl>() bind DeckRepository::class
    }
