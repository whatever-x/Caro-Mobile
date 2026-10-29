package com.whatever.caro.core.remote.datasource.auth

import com.whatever.caro.core.remote.api.AuthApi
import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationV11Request
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

internal class RemoteAuthDataSourceImpl(
    private val authApi: AuthApi,
) : AuthDataSource {
    override suspend fun completeRegistration(request: CompleteRegistrationV11Request): TokenResponse =
        authApi.requestCompleteRegistration(request = request)

    override suspend fun logout() {
        authApi.requestLogout()
    }

    override suspend fun withdraw() {
        authApi.requestWithdraw()
    }
}
