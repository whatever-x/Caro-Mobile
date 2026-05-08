package com.whatever.caro.core.data.repository

import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType

interface AuthRepository {
    suspend fun loginWithSocial(
        provider: SocialLoginType,
        idToken: String,
    ): AuthSession
}
