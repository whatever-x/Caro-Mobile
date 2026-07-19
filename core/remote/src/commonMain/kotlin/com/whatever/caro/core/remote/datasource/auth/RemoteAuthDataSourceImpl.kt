package com.whatever.caro.core.remote.datasource.auth

import com.whatever.caro.core.remote.api.AuthApi
import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationRequest
import com.whatever.caro.core.remote.dto.auth.response.TokenResponse

internal class RemoteAuthDataSourceImpl(
    private val authApi: AuthApi,
) : AuthDataSource {
    override suspend fun completeRegistration(request: CompleteRegistrationRequest): TokenResponse =
        authApi.requestCompleteRegistration(request = request)

    override suspend fun logout() {
        authApi.requestLogout()
    }
}
