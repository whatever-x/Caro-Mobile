package com.whatever.caro.core.data.repository.auth

import com.whatever.caro.core.data.mapper.toAuthSession
import com.whatever.caro.core.datastore.datasource.LocalAuthDataSource
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.model.exception.CaroAuthException
import com.whatever.caro.core.remote.datasource.auth.RemoteAuthDataSource
import com.whatever.caro.core.remote.datasource.auth.RemoteNonAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.CompleteRegistrationRequest
import com.whatever.caro.core.remote.dto.auth.request.RefreshTokenRequest
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest

internal class AuthRepositoryImpl(
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val remoteNonAuthDataSource: RemoteNonAuthDataSource,
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
        val response = remoteNonAuthDataSource.socialLogin(request = request)
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

    override suspend fun refreshToken() {
        val accessToken = localAuthDataSource.fetchAccessToken()
        val refreshToken = localAuthDataSource.fetchRefreshToken()

        if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
            throw CaroAuthException.TokenEmpty(
                debugMessage = "Token is Empty",
            )
        }
        val request =
            RefreshTokenRequest(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        remoteNonAuthDataSource
            .refreshToken(
                request = request,
            ).also {
                localAuthDataSource.saveTokens(
                    accessToken = it.accessToken,
                    refreshToken = it.refreshToken,
                )
            }
    }
}
