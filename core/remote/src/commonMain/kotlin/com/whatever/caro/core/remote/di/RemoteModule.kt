package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.datasource.auth.RemoteAuthDataSource
import com.whatever.caro.core.remote.datasource.auth.RemoteAuthDataSourceImpl
import com.whatever.caro.core.remote.datasource.auth.TokenRefreshDataSource
import com.whatever.caro.core.remote.datasource.auth.TokenRefreshDataSourceImpl
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val remoteModule =
    module {
        single<RemoteAuthDataSource> {
            RemoteAuthDataSourceImpl(
                authApi = get(named(NetworkClient.Caro.AUTH)),
                nonAuthApi = get(named(NetworkClient.Caro.NON_AUTH)),
            )
        }
        single<TokenRefreshDataSource> {
            TokenRefreshDataSourceImpl(
                authApi = get(named(NetworkClient.Caro.NON_AUTH)),
            )
        }
    }
