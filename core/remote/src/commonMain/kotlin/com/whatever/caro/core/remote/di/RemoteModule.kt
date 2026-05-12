package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.datasource.auth.RemoteAuthDataSource
import com.whatever.caro.core.remote.datasource.auth.createRemoteAuthDataSource
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import com.whatever.caro.core.remote.network.config.CaroNetworkConfig
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val remoteModule =
    module {
        single<Ktorfit>(named(NetworkClient.Caro.NON_AUTH)) {
            Ktorfit
                .Builder()
                .baseUrl(CaroNetworkConfig.BASE_URL)
                .httpClient(get<HttpClient>(named(NetworkClient.Caro.NON_AUTH)))
                .build()
        }

        single<Ktorfit>(named(NetworkClient.Caro.AUTH)) {
            Ktorfit
                .Builder()
                .baseUrl(CaroNetworkConfig.BASE_URL)
                .httpClient(get<HttpClient>(named(NetworkClient.Caro.AUTH)))
                .build()
        }

        single<RemoteAuthDataSource> {
            get<Ktorfit>(named(NetworkClient.Caro.NON_AUTH)).createRemoteAuthDataSource()
        }
    }
