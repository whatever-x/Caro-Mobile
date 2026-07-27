package com.whatever.caro.core.data.repository.auth

import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType

interface AuthRepository {
    suspend fun loginWithSocial(
        provider: SocialLoginType,
        idToken: String,
    ): AuthSession

    suspend fun logout()

    suspend fun withdraw()

    suspend fun completeRegistration(
        nickname: String,
        termsAgreed: Boolean,
    ): AuthSession

    suspend fun refreshToken()
}
