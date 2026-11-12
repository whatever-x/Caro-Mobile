package com.whatever.caro.core.remote.network.plugins

import com.whatever.caro.core.remote.auth.AuthTokenProvider
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AuthInterceptorConfig {
    lateinit var tokenProvider: AuthTokenProvider
    var refreshOnUnauthorized: (HttpRequestBuilder) -> Boolean = { true }
}

internal val AuthInterceptorPlugin =
    createClientPlugin(
        name = "CaroAuthInterceptor",
        createConfiguration = ::AuthInterceptorConfig,
    ) {
        val tokenProvider = pluginConfig.tokenProvider
        val refreshMutex = Mutex()

        on(Send) { request ->
            val sessionSnapshot = tokenProvider.getSessionSnapshot()
            val accessTokenSnapshot = sessionSnapshot.accessToken
            if (!accessTokenSnapshot.isNullOrEmpty()) {
                request.headers {
                    set(HttpHeaders.Authorization, "Bearer $accessTokenSnapshot")
                }
            }

            var call = proceed(request)
            // Pending signups keep their input and choose when to sign in again.
            if (call.response.status != HttpStatusCode.Unauthorized ||
                !sessionSnapshot.isRegistrationComplete ||
                !pluginConfig.refreshOnUnauthorized(request)
            ) {
                return@on call
            }

            val refreshedAccessToken =
                refreshMutex.withLock {
                    val current = tokenProvider.getSessionSnapshot()
                    if (current.generation != sessionSnapshot.generation) {
                        null
                    } else {
                        val access =
                            if (!current.accessToken.isNullOrEmpty() && current.accessToken != accessTokenSnapshot) {
                                current.accessToken
                            } else {
                                tokenProvider.refresh(sessionSnapshot.generation)
                            }
                        access.takeIf { tokenProvider.getSessionSnapshot().generation == sessionSnapshot.generation }
                    }
                } ?: return@on call

            request.headers {
                set(HttpHeaders.Authorization, "Bearer $refreshedAccessToken")
            }
            call = proceed(request)
            call
        }
    }
