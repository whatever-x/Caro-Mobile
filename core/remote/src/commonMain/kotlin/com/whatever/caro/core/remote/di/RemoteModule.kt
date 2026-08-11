package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.datasource.auth.AuthDataSource
import com.whatever.caro.core.remote.datasource.auth.NonAuthDataSource
import com.whatever.caro.core.remote.datasource.auth.RemoteAuthDataSourceImpl
import com.whatever.caro.core.remote.datasource.auth.RemoteNonAuthDataSourceImpl
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.datasource.card.RemoteCardDataSourceImpl
import com.whatever.caro.core.remote.datasource.deck.DeckDataSource
import com.whatever.caro.core.remote.datasource.deck.RemoteDeckDataSourceImpl
import com.whatever.caro.core.remote.datasource.profile.ProfileDataSource
import com.whatever.caro.core.remote.datasource.profile.RemoteProfileDataSourceImpl
import com.whatever.caro.core.remote.datasource.streak.RemoteStreakDataSourceImpl
import com.whatever.caro.core.remote.datasource.streak.StreakDataSource
import com.whatever.caro.core.remote.datasource.study.RemoteStudySessionDataSourceImpl
import com.whatever.caro.core.remote.datasource.study.StudySessionDataSource
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val remoteModule =
    module {
        single<AuthDataSource> {
            RemoteAuthDataSourceImpl(
                authApi = get(named(NetworkClient.Caro.AUTH)),
            )
        }

        single<NonAuthDataSource> {
            RemoteNonAuthDataSourceImpl(
                nonAuthApi = get(named(NetworkClient.Caro.NON_AUTH)),
            )
        }

        single<RemoteProfileDataSourceImpl>() bind ProfileDataSource::class

        single<RemoteCardDataSourceImpl>() bind CardDataSource::class
        single<RemoteDeckDataSourceImpl>() bind DeckDataSource::class
        single<RemoteStreakDataSourceImpl>() bind StreakDataSource::class
        single<RemoteStudySessionDataSourceImpl>() bind StudySessionDataSource::class
    }
