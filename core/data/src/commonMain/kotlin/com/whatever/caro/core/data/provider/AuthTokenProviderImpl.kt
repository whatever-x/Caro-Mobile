package com.whatever.caro.core.data.provider

import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.auth.AuthSessionEvent
import com.whatever.caro.core.model.auth.AuthSessionEventBus
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.remote.auth.AuthTokenProvider
import com.whatever.caro.core.remote.datasource.auth.NonAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.RefreshTokenRequest
import kotlinx.coroutines.CancellationException

internal class AuthTokenProviderImpl(
    private val localAuthDataSource: LocalAuthDataSource,
    private val remoteNonAuthDataSource: NonAuthDataSource,
    private val authSessionEventBus: AuthSessionEventBus,
) : AuthTokenProvider {
    override suspend fun getAccessToken(): String? = localAuthDataSource.fetchAccessToken()

    override suspend fun getRefreshToken(): String? = localAuthDataSource.fetchRefreshToken()

    override suspend fun refresh(): String {
        val currentRefresh = localAuthDataSource.fetchRefreshToken()
        if (currentRefresh.isNullOrBlank()) {
            notifySessionExpired()
            throw CaroAuthException.TokenExpired(
                debugMessage = "RefreshToken이 존재하지 않습니다.",
            )
        }

        return runCatching {
            val currentAccess = localAuthDataSource.fetchAccessToken().orEmpty()
            val refreshed =
                remoteNonAuthDataSource.refreshToken(
                    request =
                        RefreshTokenRequest(
                            accessToken = currentAccess,
                            refreshToken = currentRefresh,
                        ),
                )
            localAuthDataSource.saveTokens(
                accessToken = refreshed.accessToken,
                refreshToken = refreshed.refreshToken,
            )
            refreshed.accessToken
        }.getOrElse { throwable ->
            when (throwable) {
                is CancellationException -> {
                    throw throwable
                }

                else -> {
                    notifySessionExpired()
                    throw CaroAuthException.TokenExpired(
                        debugMessage = "Token 재발급 요청에 실패했습니다. cause=${throwable.message.orEmpty()}",
                        throwable = throwable,
                    )
                }
            }
        }
    }

    override suspend fun clearTokens() {
        localAuthDataSource.clear()
    }

    private suspend fun notifySessionExpired() {
        localAuthDataSource.clear()
        authSessionEventBus.publish(AuthSessionEvent.Expired)
    }
}
