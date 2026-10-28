package com.whatever.caro.core.data.provider

import com.whatever.caro.core.data.repository.ads.AdEligibilityGate
import com.whatever.caro.core.datastore.datasource.AuthTokens
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.auth.AuthSessionEvent
import com.whatever.caro.core.model.auth.AuthSessionEventBus
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.remote.auth.AuthTokenProvider
import com.whatever.caro.core.remote.auth.AuthTokenSnapshot
import com.whatever.caro.core.remote.datasource.auth.NonAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.RefreshTokenRequest
import kotlinx.coroutines.CancellationException

internal class AuthTokenProviderImpl(
    private val localAuthDataSource: LocalAuthDataSource,
    private val remoteNonAuthDataSource: NonAuthDataSource,
    private val authSessionEventBus: AuthSessionEventBus,
    private val adEligibilityGate: AdEligibilityGate,
) : AuthTokenProvider {
    override suspend fun getSessionSnapshot(): AuthTokenSnapshot =
        localAuthDataSource.fetchTokens().let { AuthTokenSnapshot(it.accessToken, it.generation, it.isRegistrationComplete != false) }

    override suspend fun getAccessToken(): String? = localAuthDataSource.fetchAccessToken()

    override suspend fun getRefreshToken(): String? = localAuthDataSource.fetchRefreshToken()

    override suspend fun refresh(expectedGeneration: Long): String {
        val before = localAuthDataSource.fetchTokens()
        if (before.generation != expectedGeneration) {
            throw CaroAuthException.TokenExpired(debugMessage = "Session changed before refresh")
        }
        adEligibilityGate.beginTransition()
        try {
            val current = localAuthDataSource.fetchTokens()
            if (current.generation != expectedGeneration) {
                throw CaroAuthException.TokenExpired(debugMessage = "Session changed before refresh")
            }
            val currentRefresh = current.refreshToken
            if (currentRefresh.isNullOrBlank()) {
                notifySessionExpired(current)
                throw CaroAuthException.TokenExpired(
                    debugMessage = "RefreshToken이 존재하지 않습니다.",
                )
            }

            return runCatching {
                val currentAccess = current.accessToken.orEmpty()
                val refreshed =
                    remoteNonAuthDataSource.refreshToken(
                        request =
                            RefreshTokenRequest(
                                accessToken = currentAccess,
                                refreshToken = currentRefresh,
                            ),
                    )
                if (!localAuthDataSource.replaceTokensIfCurrent(current, refreshed.accessToken, refreshed.refreshToken)) {
                    throw CaroAuthException.TokenExpired(debugMessage = "Session changed during refresh")
                }
                refreshed.accessToken
            }.getOrElse { throwable ->
                when (throwable) {
                    is CancellationException -> {
                        throw throwable
                    }

                    else -> {
                        notifySessionExpired(current)
                        throw CaroAuthException.TokenExpired(
                            debugMessage = "Token 재발급 요청에 실패했습니다. cause=${throwable.message.orEmpty()}",
                            throwable = throwable,
                        )
                    }
                }
            }
        } finally {
            adEligibilityGate.endTransition()
        }
    }

    override suspend fun clearTokens() {
        adEligibilityGate.revoke()
        localAuthDataSource.clear()
    }

    private suspend fun notifySessionExpired(current: AuthTokens) {
        if (localAuthDataSource.clearIfCurrent(current)) {
            authSessionEventBus.publish(AuthSessionEvent.Expired(current.generation + 1))
        }
    }
}
