package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.auth.AuthTokenProvider
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import com.whatever.caro.core.remote.network.HttpClientEngineProvider
import com.whatever.caro.core.remote.network.HttpClientFactory
import com.whatever.caro.core.remote.network.config.CaroNetworkConfig
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule =
    module {
        single<HttpClientEngine> { HttpClientEngineProvider.provide() }
        single<Json> {
            Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            }
        }

        single<HttpClient>(named(NetworkClient.Caro.NON_AUTH)) {
            HttpClientFactory.createCaroClient(
                engine = get(),
                jsonParser = get(),
                deviceIdProvider = get(),
            )
        }

        single<HttpClient>(named(NetworkClient.Caro.AUTH)) {
            HttpClientFactory.createCaroClient(
                engine = get(),
                jsonParser = get(),
                deviceIdProvider = get(),
                authTokenProvider = { get<AuthTokenProvider>() },
            )
        }

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
    }
