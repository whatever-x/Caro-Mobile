package com.whatever.caro.core.remote.network

import com.whatever.caro.core.remote.auth.AuthTokenProvider
import com.whatever.caro.core.remote.device.DeviceIdProvider
import com.whatever.caro.core.remote.network.config.CaroNetworkConfig
import com.whatever.caro.core.remote.network.locale.DeviceLocaleProvider
import com.whatever.caro.core.remote.network.plugins.AuthInterceptorPlugin
import com.whatever.caro.core.remote.network.plugins.CaroBaseResponseConverter
import com.whatever.caro.core.remote.network.plugins.configureCaroExceptionMapping
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpCallValidator
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
    private const val CLIENT_TIMEZONE_HEADER = "Client-Timezone"
    private const val ACCEPT_LANGUAGE_HEADER = "Accept-Language"
    private const val CLIENT_DEVICE_ID_HEADER = "Client-Device-Id"

    fun createCaroClient(
        engine: HttpClientEngine,
        jsonParser: Json,
        deviceIdProvider: DeviceIdProvider,
        authTokenProvider: AuthTokenProvider? = null,
        configure: HttpClientConfig<*>.() -> Unit = { },
    ): HttpClient =
        HttpClient(engine) {
            expectSuccess = true

            install(DefaultRequest) {
                url(CaroNetworkConfig.BASE_URL)
                contentType(ContentType.Application.Json)
                headers.append(ACCEPT_LANGUAGE_HEADER, DeviceLocaleProvider.acceptLanguage())
                headers.append(CLIENT_TIMEZONE_HEADER, DeviceLocaleProvider.timezone())
                headers.append(CLIENT_DEVICE_ID_HEADER, deviceIdProvider.get())
            }

            install(ContentNegotiation) {
                json(json = jsonParser)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }

            install(Logging) {
                logger = Logger.SIMPLE
                level = if (CaroNetworkConfig.isDebug) LogLevel.ALL else LogLevel.NONE
            }

            install(HttpCallValidator) {
                configureCaroExceptionMapping(jsonParser)
            }

            install(CaroBaseResponseConverter) {
                this.json = jsonParser
            }

            if (authTokenProvider != null) {
                install(AuthInterceptorPlugin) {
                    tokenProvider = authTokenProvider
                }
            }

            configure()
        }
}
