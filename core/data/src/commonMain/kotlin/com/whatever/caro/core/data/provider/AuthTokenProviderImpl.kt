package com.whatever.caro.core.data.provider

import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.exception.CaroClientException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.ErrorCode
import com.whatever.caro.core.remote.auth.AuthTokenProvider
import com.whatever.caro.core.remote.datasource.RemoteAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import kotlinx.coroutines.CancellationException

internal class AuthTokenProviderImpl(
    private val localAuthDataSource: LocalAuthDataSource,
    private val remoteAuthDataSource: RemoteAuthDataSource,
) : AuthTokenProvider {
    override suspend fun getAccessToken(): String? = localAuthDataSource.fetchAccessToken()

    override suspend fun getRefreshToken(): String? = localAuthDataSource.fetchRefreshToken()

    override suspend fun refresh(): String {
        val currentRefresh =
            localAuthDataSource.fetchRefreshToken()
                ?: throw CaroClientException(
                    code = ErrorCode.AUTH_REFRESH_FAILED,
                    message = "Token refresh failed",
                    debugMessage = "RefreshToken이 존재하지 않습니다.",
                )
        val currentAccess = localAuthDataSource.fetchAccessToken().orEmpty()

        val refreshed =
            try {
                remoteAuthDataSource.refreshToken(
                    request =
                        TokenRefreshRequest(
                            accessToken = currentAccess,
                            refreshToken = currentRefresh,
                        ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (cause: Throwable) {
                localAuthDataSource.clear()
                throw CaroServerException(
                    code = ErrorCode.AUTH_REFRESH_FAILED,
                    message = "Token refresh failed",
                    debugMessage = "Refresh API 호출에 실패했습니다: ${cause.message.orEmpty()}",
                    throwable = cause,
                )
            }

        localAuthDataSource.saveTokens(
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken,
        )
        return refreshed.accessToken
    }

    override suspend fun clearTokens() {
        localAuthDataSource.clear()
    }
}
