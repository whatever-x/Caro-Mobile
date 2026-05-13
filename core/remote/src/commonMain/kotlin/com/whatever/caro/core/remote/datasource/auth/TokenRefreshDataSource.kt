package com.whatever.caro.core.remote.datasource.auth

import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

interface TokenRefreshDataSource {
    suspend fun refreshToken(request: TokenRefreshRequest): TokenResponse
}
