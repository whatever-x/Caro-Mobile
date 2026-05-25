package com.whatever.caro.core.data.provider

import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.exception.CaroClientException
import com.whatever.caro.core.model.exception.ErrorCode
import com.whatever.caro.core.remote.auth.AuthTokenProvider
import com.whatever.caro.core.remote.datasource.RemoteNonAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import kotlinx.coroutines.CancellationException

internal class AuthTokenProviderImpl(
    private val localAuthDataSource: LocalAuthDataSource,
    private val remoteNonAuthDatasource: RemoteNonAuthDataSource,
) : AuthTokenProvider {
    override suspend fun getAccessToken(): String? = localAuthDataSource.fetchAccessToken()

    override suspend fun getRefreshToken(): String? = localAuthDataSource.fetchRefreshToken()

    override suspend fun refresh(): String =
        runCatching {
            // TODO: ViewModel 전역 예외처리 핸들러 구현 후 처리필요
            val currentRefresh =
                localAuthDataSource.fetchRefreshToken()
                    ?: throw CaroClientException(
                        code = ErrorCode.AUTH_REFRESH_FAILED,
                        message = "Token refresh failed",
                        debugMessage = "RefreshToken이 존재하지 않습니다.",
                    )
            val currentAccess = localAuthDataSource.fetchAccessToken().orEmpty()
            val refreshed =
                remoteNonAuthDatasource.refreshToken(
                    request =
                        TokenRefreshRequest(
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
                is CancellationException -> throw throwable

                // TODO: ViewModel 전역 예외처리 핸들러 구현 후 처리필요
                else -> throw CaroClientException(
                    code = "",
                    message = "",
                    debugMessage = "",
                )
            }
        }

    override suspend fun clearTokens() {
        localAuthDataSource.clear()
    }
}
