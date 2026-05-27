package com.whatever.caro.core.remote.network.plugins

import com.whatever.caro.core.remote.auth.AuthTokenProvider
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AuthInterceptorConfig {
    lateinit var tokenProvider: AuthTokenProvider
}

internal val AuthInterceptorPlugin =
    createClientPlugin(
        name = "CaroAuthInterceptor",
        createConfiguration = ::AuthInterceptorConfig,
    ) {
        val tokenProvider = pluginConfig.tokenProvider
        val refreshMutex = Mutex()

        on(Send) { request ->
            val accessTokenSnapshot = tokenProvider.getAccessToken()
            if (!accessTokenSnapshot.isNullOrEmpty()) {
                request.headers {
                    set(HttpHeaders.Authorization, "Bearer $accessTokenSnapshot")
                }
            }

            var call = proceed(request)
            if (call.response.status != HttpStatusCode.Unauthorized) {
                return@on call
            }

            val refreshedAccessToken =
                refreshMutex.withLock {
                    val current = tokenProvider.getAccessToken()
                    if (!current.isNullOrEmpty() && current != accessTokenSnapshot) {
                        current
                    } else {
                        tokenProvider.refresh()
                    }
                }

            request.headers {
                set(HttpHeaders.Authorization, "Bearer $refreshedAccessToken")
            }
            call = proceed(request)
            call
        }
    }
