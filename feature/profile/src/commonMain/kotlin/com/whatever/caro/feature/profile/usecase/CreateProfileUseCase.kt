package com.whatever.caro.feature.profile.usecase

import com.whatever.caro.core.data.repository.AuthRepository

class CreateProfileUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        nickname: String,
        termsAgreed: Boolean,
    ) {
        authRepository.completeRegistration(
            nickname = nickname,
            termsAgreed = termsAgreed,
        )
    }
}
