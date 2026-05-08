package com.whatever.caro.core.remote.datasource.auth

import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest
import com.whatever.caro.core.remote.dto.auth.request.TokenRefreshRequest
import com.whatever.caro.core.remote.dto.auth.response.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

internal class RemoteAuthDataSourceImpl(
    private val defaultClient: HttpClient,
) : RemoteAuthDataSource {
    override suspend fun login(request: SocialLoginRequest): LoginResponse =
        defaultClient
            .post(LOGIN_PATH) {
                setBody(body = request)
            }.body()

    override suspend fun refresh(request: TokenRefreshRequest): LoginResponse =
        defaultClient
            .post(REFRESH_PATH) {
                setBody(body = request)
            }.body()

    private companion object {
        private const val LOGIN_PATH = "v1/auth/social-login"
        private const val REFRESH_PATH = "v1/auth/refresh"
    }
}
