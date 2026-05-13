package com.whatever.caro.core.remote.datasource.auth

import com.whatever.caro.core.remote.api.AuthApi
import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

internal class TokenRefreshDataSourceImpl(
    private val authApi: AuthApi,
) : TokenRefreshDataSource {
    override suspend fun refreshToken(request: TokenRefreshRequest): TokenResponse = authApi.requestTokenRefresh(request = request)
}
