package com.whatever.caro.feature.profile.usecase

import com.whatever.caro.core.data.repository.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import org.koin.core.annotation.Single

@Single
class CheckNicknameUseCase(
    private val profileRepository: ProfileRepository,
) {
    operator fun invoke(nicknameFlow: Flow<String>): Flow<NicknameValidationResult> =
        nicknameFlow
            .debounce(DEBOUNCE_MS)
            .mapLatest { nickname ->
                val validation = profileRepository.validateNickname(nickname)
                if (validation.isValid) {
                    return@mapLatest NicknameValidationResult.Valid
                }
                when (validation.reason) {
                    "TOO_SHORT" -> NicknameValidationResult.TooShort
                    "TOO_LONG" -> NicknameValidationResult.TooLong
                    "INVALID_CHARACTER" -> NicknameValidationResult.InvalidCharacter
                    "DUPLICATE" -> NicknameValidationResult.Duplicate
                    else -> NicknameValidationResult.InvalidCharacter
                }
            }

    companion object {
        private const val DEBOUNCE_MS = 350L
    }
}
