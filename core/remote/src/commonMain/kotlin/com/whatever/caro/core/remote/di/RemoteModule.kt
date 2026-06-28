package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.datasource.RemoteAuthDataSource
import com.whatever.caro.core.remote.datasource.RemoteAuthDataSourceImpl
import com.whatever.caro.core.remote.datasource.RemoteNonAuthDataSource
import com.whatever.caro.core.remote.datasource.RemoteNonAuthDataSourceImpl
import com.whatever.caro.core.remote.datasource.card.CardDataSource
import com.whatever.caro.core.remote.datasource.card.RemoteCardDataSourceImpl
import com.whatever.caro.core.remote.datasource.deck.DeckDataSource
import com.whatever.caro.core.remote.datasource.deck.RemoteDeckDataSourceImpl
import com.whatever.caro.core.remote.datasource.profile.ProfileDataSource
import com.whatever.caro.core.remote.datasource.profile.RemoteProfileDataSourceImpl
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val remoteModule =
    module {
        single<RemoteAuthDataSource> {
            RemoteAuthDataSourceImpl(
                authApi = get(named(NetworkClient.Caro.AUTH)),
            )
        }

        single<RemoteNonAuthDataSource> {
            RemoteNonAuthDataSourceImpl(
                nonAuthApi = get(named(NetworkClient.Caro.NON_AUTH)),
            )
        }

        single<RemoteProfileDataSourceImpl>() bind ProfileDataSource::class

        single<RemoteCardDataSourceImpl>() bind CardDataSource::class
        single<RemoteDeckDataSourceImpl>() bind DeckDataSource::class
    }
