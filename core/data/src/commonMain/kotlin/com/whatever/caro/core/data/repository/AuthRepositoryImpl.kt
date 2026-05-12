package com.whatever.caro.core.data.repository

import com.whatever.caro.core.data.mapper.toAuthSession
import com.whatever.caro.core.datastore.auth.TokenLocalDataSource
import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.remote.datasource.auth.RemoteAuthDataSource
import com.whatever.caro.core.remote.dto.auth.request.SocialLoginRequest

internal class AuthRepositoryImpl(
    private val remoteAuthDataSource: RemoteAuthDataSource,
    private val tokenLocalDataSource: TokenLocalDataSource,
) : AuthRepository {
    override suspend fun loginWithSocial(
        provider: SocialLoginType,
        idToken: String,
    ): AuthSession {
        val response =
            remoteAuthDataSource.login(
                request =
                    SocialLoginRequest(
                        provider = provider,
                        idToken = idToken,
                    ),
            )
        tokenLocalDataSource.saveTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
        )
        return response.toAuthSession()
    }
}
