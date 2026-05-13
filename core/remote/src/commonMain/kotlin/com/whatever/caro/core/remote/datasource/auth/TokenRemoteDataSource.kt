package com.whatever.caro.core.remote.datasource.auth

import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

interface TokenRemoteDataSource {
    suspend fun refreshToken(request: TokenRefreshRequest): TokenResponse
}
