package com.whatever.caro.core.remote.di

import com.whatever.caro.core.remote.di.qualifier.CaroClient
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import com.whatever.caro.core.remote.network.HttpClientEngineProvider
import com.whatever.caro.core.remote.network.HttpClientFactory
import io.ktor.client.HttpClient
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

@Module
class NetworkModule {

    @Single
    fun providesHttpClientEngine(): HttpClientEngineProvider = HttpClientEngineProvider()

    /**
     * Auth 설정없는 기본 클라이언트
     */
    @Single
    @NetworkClient(CaroClient.Default)
    fun defaultClient(
        engineProvider: HttpClientEngineProvider,
    ): HttpClient =
        HttpClientFactory.create(engine = engineProvider.createEngine())
            .config {
                // TODO : Ktor 커스텀 플러그인 등록
            }

    /**
     * Auth 설정된 기본 클라이언트
     */
    @Single
    @NetworkClient(CaroClient.Auth)
    fun authClient(
        engineProvider: HttpClientEngineProvider,
    ): HttpClient =
        HttpClientFactory.create(engine = engineProvider.createEngine())
            .config {
                // TODO : Ktor 커스텀 플러그인 등록
            }

}