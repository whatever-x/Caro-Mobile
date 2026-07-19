package com.whatever.caro.core.remote.datasource.auth

import com.whatever.caro.core.remote.dto.auth.request.RefreshTokenRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import com.whatever.caro.core.remote.dto.auth.response.SocialLoginResponse
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

interface NonAuthDataSource {
    suspend fun refreshToken(request: RefreshTokenRequest): TokenResponse

    suspend fun socialLogin(request: SocialLoginRequest): SocialLoginResponse
}
