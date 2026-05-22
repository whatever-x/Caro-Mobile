package com.whatever.caro.core.data.repository

import com.whatever.caro.core.data.mapper.toAuthSession
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.remote.datasource.RemoteAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest

internal class AuthRepositoryImpl(
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val localAuthDataSource: LocalAuthDataSource,
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
        localAuthDataSource.saveTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
        )
        return response.toAuthSession()
    }

    override suspend fun logout() {
        remoteAuthDataSource.logout()
        localAuthDataSource.clear()
    }

    override suspend fun completeRegistration(
        nickname: String,
        termsAgreed: Boolean,
    ): AuthSession {
        val request =
            CompleteRegistrationRequest(
                nickname = nickname,
                isTermsAgreed = termsAgreed,
            )
        val response = remoteAuthDataSource.completeRegistration(request = request)
        localAuthDataSource.saveTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
        )
        return response.toAuthSession()
    }
}
