package com.whatever.caro.core.remote.api

import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationRequest
import com.whatever.caro.core.remote.dto.auth.request.RefreshTokenRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import com.whatever.caro.core.remote.dto.auth.response.SocialLoginResponse
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST

internal interface AuthApi {
    @Headers(ApiVersionHeaders.V1_0)
    @POST("auth/social-login")
    suspend fun requestSocialLogin(
        @Body request: SocialLoginRequest,
    ): SocialLoginResponse

    @Headers(ApiVersionHeaders.V1_0)
    @POST("auth/refresh")
    suspend fun requestRefreshToken(
        @Body request: RefreshTokenRequest,
    ): TokenResponse

    @Headers(ApiVersionHeaders.V1_0)
    @POST("auth/complete-registration")
    suspend fun requestCompleteRegistration(
        @Body request: CompleteRegistrationRequest,
    ): TokenResponse

    @Headers(ApiVersionHeaders.V1_0)
    @POST("auth/logout")
    suspend fun requestLogout()

    @Headers(ApiVersionHeaders.V1_0)
    @DELETE("auth/withdraw")
    suspend fun requestWithdraw()
}
