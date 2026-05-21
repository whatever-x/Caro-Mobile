package com.whatever.caro.feature.profile.usecase

import com.whatever.caro.core.data.repository.profile.ProfileRepository

class CheckNicknameUseCase(
    private val profileRepository: ProfileRepository,
) {
    suspend operator fun invoke(nickname: String): NicknameValidationResult {
        val validation = profileRepository.validateNickname(nickname)
        if (validation.isValid) return NicknameValidationResult.Valid
        return when (validation.reason) {
            "TOO_SHORT" -> NicknameValidationResult.TooShort
            "TOO_LONG" -> NicknameValidationResult.TooLong
            "INVALID_CHARACTER" -> NicknameValidationResult.InvalidCharacter
            "DUPLICATE" -> NicknameValidationResult.Duplicate
            else -> NicknameValidationResult.InvalidCharacter
        }
    }
}
