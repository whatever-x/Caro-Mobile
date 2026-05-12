package com.whatever.caro.core.remote.datasource.auth

import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import com.whatever.caro.core.remote.dto.auth.response.LoginResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST

interface RemoteAuthDataSource {
    @POST("v1/auth/social-login")
    suspend fun login(
        @Body request: SocialLoginRequest,
    ): LoginResponse

    @POST("v1/auth/refresh")
    suspend fun refresh(
        @Body request: TokenRefreshRequest,
    ): LoginResponse
}
