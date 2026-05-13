package com.whatever.caro.core.data.auth

import com.whatever.caro.core.datastore.auth.TokenLocalDataSource
import com.whatever.caro.core.model.exception.CaroClientException
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.ErrorCode
import com.whatever.caro.core.remote.auth.AuthTokenProvider
import com.whatever.caro.core.remote.datasource.auth.TokenRefreshDataSource
import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import kotlinx.coroutines.CancellationException

internal class AuthTokenProviderImpl(
    private val tokenLocalDataSource: TokenLocalDataSource,
    private val tokenRefreshDataSource: TokenRefreshDataSource,
) : AuthTokenProvider {
    override suspend fun getAccessToken(): String? = tokenLocalDataSource.fetchAccessToken()

    override suspend fun getRefreshToken(): String? = tokenLocalDataSource.fetchRefreshToken()

    override suspend fun refresh(): String {
        val currentRefresh =
            tokenLocalDataSource.fetchRefreshToken()
                ?: throw CaroClientException(
                    code = ErrorCode.AUTH_REFRESH_FAILED,
                    message = "Token refresh failed",
                    debugMessage = "RefreshToken이 존재하지 않습니다.",
                )
        val currentAccess = tokenLocalDataSource.fetchAccessToken().orEmpty()

        val refreshed =
            try {
                tokenRefreshDataSource.refreshToken(
                    request =
                        TokenRefreshRequest(
                            accessToken = currentAccess,
                            refreshToken = currentRefresh,
                        ),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (cause: Throwable) {
                tokenLocalDataSource.clear()
                throw CaroServerException(
                    code = ErrorCode.AUTH_REFRESH_FAILED,
                    message = "Token refresh failed",
                    debugMessage = "Refresh API 호출에 실패했습니다: ${cause.message.orEmpty()}",
                    throwable = cause,
                )
            }

        tokenLocalDataSource.saveTokens(
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken,
        )
        return refreshed.accessToken
    }

    override suspend fun clearTokens() {
        tokenLocalDataSource.clear()
    }
}
