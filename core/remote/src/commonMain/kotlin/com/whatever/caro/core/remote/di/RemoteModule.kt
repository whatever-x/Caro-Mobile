package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.datasource.RemoteAuthDataSource
import com.whatever.caro.core.remote.datasource.RemoteAuthDataSourceImpl
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
    }
