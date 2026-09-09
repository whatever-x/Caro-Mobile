package com.whatever.caro.core.data.repository.auth

import com.whatever.caro.core.model.auth.AuthSession
import com.whatever.caro.core.model.auth.SocialLoginType

interface AuthRepository {
    suspend fun loginWithSocial(
        provider: SocialLoginType,
        idToken: String,
    ): Boolean

    suspend fun logout()

    suspend fun withdraw()

    suspend fun completeRegistration(
        nickname: String,
        termsAgreed: Boolean,
    ): AuthSession

    /**
     * 소셜 로그인만 끝내고 닉네임 설정을 마치지 않은 세션인지 판별한다.
     * 저장된 값이 없으면(플래그 도입 이전 사용자) 가입 완료로 본다.
     */
    suspend fun isRegistrationComplete(): Boolean

    suspend fun refreshToken()
}
