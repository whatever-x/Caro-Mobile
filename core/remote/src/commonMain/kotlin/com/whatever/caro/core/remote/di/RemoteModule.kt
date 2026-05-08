package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.datasource.auth.RemoteAuthDataSource
import com.whatever.caro.core.remote.datasource.auth.RemoteAuthDataSourceImpl
import com.whatever.caro.core.remote.datasource.sample.SampleDataSource
import com.whatever.caro.core.remote.datasource.sample.SampleDataSourceImpl
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val remoteModule =
    module {
        single<SampleDataSource> {
            SampleDataSourceImpl(
                httpClient = get(named(NetworkClient.Caro.NON_AUTH)),
            )
        }

        single<RemoteAuthDataSource> {
            RemoteAuthDataSourceImpl(
                defaultClient = get(named(NetworkClient.Caro.NON_AUTH)),
            )
        }
    }
