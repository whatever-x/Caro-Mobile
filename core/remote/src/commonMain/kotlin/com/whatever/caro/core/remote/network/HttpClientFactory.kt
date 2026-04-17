package com.whatever.caro.core.remote.network

import com.whatever.caro.core.remote.generated.BuildKonfig
import com.whatever.caro.core.remote.network.config.CaroNetworkConfig
import com.whatever.caro.core.remote.network.plugins.CaroBaseResponseUnwrap
import com.whatever.caro.core.remote.network.plugins.CaroResponseValidator
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {

    fun createCaroClient(
        engine: HttpClientEngine,
        json: Json,
        configure: HttpClientConfig<*>.() -> Unit = { }
    ): HttpClient =
        HttpClient(engine) {
            expectSuccess = true

            install(DefaultRequest) {
                url(CaroNetworkConfig.BASE_URL)
                contentType(ContentType.Application.Json)
            }

            install(ContentNegotiation) {
                json(json = json)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }

            install(Logging) {
                logger = Logger.SIMPLE
                level = if (BuildKonfig.IS_DEBUG) LogLevel.ALL else LogLevel.NONE
            }

            install(CaroResponseValidator)

            install(CaroBaseResponseUnwrap) {
                this.json = json
            }

            configure()
        }

}
