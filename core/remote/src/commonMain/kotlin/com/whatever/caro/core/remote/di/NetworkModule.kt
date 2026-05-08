package com.whatever.caro.core.remote.di

import com.whatever.caro.core.model.exception.ErrorCode
import com.whatever.caro.core.model.exception.TokenRefreshException
import com.whatever.caro.core.remote.auth.InMemoryTokenStore
import com.whatever.caro.core.remote.auth.TokenStore
import com.whatever.caro.core.remote.datasource.auth.RemoteAuthDataSource
import com.whatever.caro.core.remote.di.qualifier.NetworkClient
import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import com.whatever.caro.core.remote.network.HttpClientEngineProvider
import com.whatever.caro.core.remote.network.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProviders
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.bind
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

        single { InMemoryTokenStore() } bind TokenStore::class

        single<HttpClient>(named(NetworkClient.Caro.NON_AUTH)) {
            HttpClientFactory.createCaroClient(
                engine = get(),
                jsonParser = get(),
                deviceIdProvider = get(),
            )
        }

        single<HttpClient>(named(NetworkClient.Caro.AUTH)) {
            val tokenStore: InMemoryTokenStore = get()
            val refreshDataSource: RemoteAuthDataSource = get()

            val client =
                HttpClientFactory.createCaroClient(
                    engine = get(),
                    jsonParser = get(),
                    deviceIdProvider = get(),
                ) {
                    install(Auth) {
                        bearer {
                            loadTokens {
                                val accessToken = tokenStore.getAccessToken()
                                val refreshToken = tokenStore.getRefreshToken()
                                if (accessToken != null && refreshToken != null) {
                                    BearerTokens(accessToken, refreshToken)
                                } else {
                                    null
                                }
                            }

                            refreshTokens {
                                val currentRefresh =
                                    tokenStore.getRefreshToken()
                                        ?: throw TokenRefreshException(
                                            code = ErrorCode.AUTH_REFRESH_FAILED,
                                            message = "Token refresh failed",
                                            debugMessage = "RefreshToken이 존재하지 않습니다.",
                                        )
                                val currentAccess = tokenStore.getAccessToken().orEmpty()

                                val refreshed =
                                    try {
                                        refreshDataSource.refresh(
                                            request =
                                                TokenRefreshRequest(
                                                    accessToken = currentAccess,
                                                    refreshToken = currentRefresh,
                                                ),
                                        )
                                    } catch (cancellation: CancellationException) {
                                        throw cancellation
                                    } catch (cause: Throwable) {
                                        tokenStore.clear()
                                        throw TokenRefreshException(
                                            code = ErrorCode.AUTH_REFRESH_FAILED,
                                            message = "Token refresh failed",
                                            debugMessage = "Refresh API 호출에 실패했습니다: ${cause.message.orEmpty()}",
                                            throwable = cause,
                                        )
                                    }

                                tokenStore.save(
                                    accessToken = refreshed.accessToken,
                                    refreshToken = refreshed.refreshToken,
                                )
                                BearerTokens(refreshed.accessToken, refreshed.refreshToken)
                            }

                            sendWithoutRequest { true }
                        }
                    }
                }

            tokenStore.setOnTokensChanged {
                client.authProviders
                    .filterIsInstance<BearerAuthProvider>()
                    .forEach { it.clearToken() }
            }

            client
        }
    }
