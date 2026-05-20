package com.whatever.caro.core.remote.datasource

import com.whatever.caro.core.remote.api.AuthApi
import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import com.whatever.caro.core.remote.dto.auth.response.SocialLoginResponse
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

internal class RemoteAuthDataSourceImpl(
    private val authApi: AuthApi,
    private val nonAuthApi: AuthApi,
) : RemoteAuthDataSource {
    override suspend fun refreshToken(request: TokenRefreshRequest): TokenResponse = nonAuthApi.requestTokenRefresh(request = request)

    override suspend fun socialLogin(request: SocialLoginRequest): SocialLoginResponse = nonAuthApi.requestSocialLogin(request = request)

    override suspend fun completeRegistration(request: CompleteRegistrationRequest): TokenResponse =
        authApi.requestCompleteRegistration(request = request)

    override suspend fun logout() {
        authApi.requestLogout()
    }
}
