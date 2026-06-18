package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationRequest
import com.whatever.caro.core.remote.dto.auth.request.RefreshTokenRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import com.whatever.caro.core.remote.dto.auth.response.SocialLoginResponse
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST

internal interface AuthApi {
    @POST("v1/auth/social-login")
    suspend fun requestSocialLogin(
        @Body request: SocialLoginRequest,
    ): SocialLoginResponse

    @POST("v1/auth/refresh")
    suspend fun requestRefreshToken(
        @Body request: RefreshTokenRequest,
    ): TokenResponse

    @POST("v1/auth/complete-registration")
    suspend fun requestCompleteRegistration(
        @Body request: CompleteRegistrationRequest,
    ): TokenResponse

    @POST("v1/auth/logout")
    suspend fun requestLogout()
}
