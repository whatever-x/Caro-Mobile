package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.datasource.demo.DemoDataSource
import com.whatever.caro.core.remote.datasource.demo.DemoDataSourceImpl
import com.whatever.caro.core.remote.datasource.profile.ProfileDataSource
import com.whatever.caro.core.remote.datasource.profile.ProfileDataSourceImpl
import com.whatever.caro.core.remote.datasource.sample.SampleDataSource
import com.whatever.caro.core.remote.datasource.sample.SampleDataSourceImpl
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val remoteModule =
    module {
        single<DemoDataSource> {
            DemoDataSourceImpl(
                authClient = get(named(NetworkClient.Caro.AUTH)),
                defaultClient = get(named(NetworkClient.Caro.NON_AUTH)),
            )
        }

        single<SampleDataSource> {
            SampleDataSourceImpl(
                httpClient = get(named(NetworkClient.Caro.NON_AUTH)),
            )
        }

        single<ProfileDataSource> { ProfileDataSourceImpl() }
    }
