package com.whatever.caro.core.remote.network.plugins

import com.whatever.caro.core.remote.auth.AuthTokenProvider
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AuthInterceptorConfig {
    lateinit var tokenProvider: () -> AuthTokenProvider
}

internal val SkipAuthAttributeKey: AttributeKey<Unit> = AttributeKey("CaroSkipAuth")

internal val AuthInterceptorPlugin =
    createClientPlugin(
        name = "CaroAuthInterceptor",
        createConfiguration = ::AuthInterceptorConfig,
    ) {
        val tokenProviderFactory = pluginConfig.tokenProvider
        val refreshMutex = Mutex()

        on(Send) { request ->
            val tokenProvider = tokenProviderFactory()
            val skipAuth = request.attributes.contains(SkipAuthAttributeKey)

            val accessTokenSnapshot =
                if (skipAuth) {
                    null
                } else {
                    tokenProvider.getAccessToken()
                }

            if (!accessTokenSnapshot.isNullOrEmpty()) {
                request.headers {
                    set(HttpHeaders.Authorization, "Bearer $accessTokenSnapshot")
                }
            }

            var call = proceed(request)

            if (skipAuth || call.response.status != HttpStatusCode.Unauthorized) {
                return@on call
            }

            val refreshedAccessToken =
                refreshMutex.withLock {
                    val current = tokenProvider.getAccessToken()
                    if (!current.isNullOrEmpty() && current != accessTokenSnapshot) {
                        current
                    } else {
                        try {
                            tokenProvider.refresh()
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (cause: Throwable) {
                            tokenProvider.clearTokens()
                            throw cause
                        }
                    }
                }

            request.headers {
                set(HttpHeaders.Authorization, "Bearer $refreshedAccessToken")
            }
            call = proceed(request)
            call
        }
    }
