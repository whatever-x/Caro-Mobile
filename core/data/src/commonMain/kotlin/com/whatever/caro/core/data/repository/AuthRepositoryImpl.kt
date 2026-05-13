package com.whatever.caro.core.data.repository

import com.whatever.caro.core.data.mapper.toAuthSession
import com.whatever.caro.core.datastore.datasource.TokenLocalDataSource
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.remote.datasource.auth.RemoteAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest

internal class AuthRepositoryImpl(
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val tokenLocalDataSource: TokenLocalDataSource,
) : AuthRepository {
    override suspend fun loginWithSocial(
        provider: SocialLoginType,
        idToken: String,
    ): AuthSession {
        val request =
            SocialLoginRequest(
                provider = provider,
                idToken = idToken,
            )
        val response = remoteAuthDataSource.socialLogin(request = request)
        tokenLocalDataSource.saveTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
        )
        return response.toAuthSession()
    }

    override suspend fun logout() {
        remoteAuthDataSource.logout()
        tokenLocalDataSource.clear()
    }

    override suspend fun completeRegistration(
        nickname: String,
        termsAgreed: Boolean,
    ): AuthSession {
        val request =
            CompleteRegistrationRequest(
                nickname = nickname,
                termsAgreed = termsAgreed,
            )
        val response = remoteAuthDataSource.completeRegistration(request = request)
        return response.toAuthSession()
    }
}
