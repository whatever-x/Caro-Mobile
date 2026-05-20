package com.whatever.caro.core.remote.datasource

import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import com.whatever.caro.core.remote.dto.auth.response.SocialLoginResponse
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

interface RemoteAuthDataSource {
    suspend fun refreshToken(request: TokenRefreshRequest): TokenResponse

    suspend fun socialLogin(request: SocialLoginRequest): SocialLoginResponse

    suspend fun completeRegistration(request: CompleteRegistrationRequest): TokenResponse

    suspend fun logout()
}
