package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import com.whatever.caro.core.remote.network.HttpClientEngineProvider
import com.whatever.caro.core.remote.network.HttpClientFactory
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule =
    module {
        single<HttpClientEngine> { HttpClientEngineProvider().createEngine() }

        single(named(NetworkClient.AUTH)) {
            HttpClientFactory
                .create(engine = get())
                .config {
                    // TODO : 인증 + Ktor 커스텀 플러그인 등록
                }
        }

        single(named(NetworkClient.DEFAULT)) {
            HttpClientFactory
                .create(engine = get())
                .config {
                    // TODO : Ktor 커스텀 플러그인 등록
                }
        }
    }
