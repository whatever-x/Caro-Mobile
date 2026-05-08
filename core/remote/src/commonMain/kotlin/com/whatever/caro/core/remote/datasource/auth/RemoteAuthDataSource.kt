package com.whatever.caro.core.remote.datasource.auth

import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import com.whatever.caro.core.remote.dto.auth.response.LoginResponse

interface RemoteAuthDataSource {
    suspend fun login(request: SocialLoginRequest): LoginResponse

    suspend fun refresh(request: TokenRefreshRequest): LoginResponse
}
