package com.whatever.caro.core.remote.datasource.auth

import com.whatever.caro.core.remote.api.AuthApi
import com.whatever.caro.core.remote.dto.auth.request.RefreshTokenRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import com.whatever.caro.core.remote.dto.auth.response.SocialLoginResponse
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

internal class RemoteNonAuthDataSourceImpl(
    private val nonAuthApi: AuthApi,
) : NonAuthDataSource {
    override suspend fun refreshToken(request: RefreshTokenRequest): TokenResponse = nonAuthApi.requestRefreshToken(request = request)

    override suspend fun socialLogin(request: SocialLoginRequest): SocialLoginResponse = nonAuthApi.requestSocialLogin(request = request)
}
