package com.whatever.caro.feature.profile.usecase

import org.koin.core.annotation.Single

@Single
class CreateProfileUseCase(
    private val validateNicknameUseCase: ValidateNicknameUseCase,
) {
    suspend operator fun invoke(nickname: String): CreateProfileResult {
        val validation = validateNicknameUseCase.validate(nickname)
        if (!validation.isValid) return CreateProfileResult.InvalidNickname(validation)

        // TODO: 프로필 생성 API 연동
        return CreateProfileResult.Success
    }
}

sealed interface CreateProfileResult {
    data object Success : CreateProfileResult

    data class InvalidNickname(
        val reason: NicknameValidationResult,
    ) : CreateProfileResult
}
