package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import com.whatever.caro.core.remote.network.HttpClientEngineProvider
import com.whatever.caro.core.remote.network.HttpClientFactory
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

        single<HttpClient>(named(NetworkClient.Caro.AUTH)) {
            HttpClientFactory.createCaroClient(
                engine = get(),
                jsonParser = get(),
            ) {
                // TODO : AUTH 전용 플러그인 추가
            }
        }

        single<HttpClient>(named(NetworkClient.Caro.NON_AUTH)) {
            HttpClientFactory.createCaroClient(
                engine = get(),
                jsonParser = get(),
            ) {
                // TODO : NON_AUTH 전용 플러그인 추가
            }
        }
    }
