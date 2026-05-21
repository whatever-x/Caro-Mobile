package com.whatever.caro.feature.profile.usecase

class ValidateNicknameUseCase {
    private val nicknameRegex = Regex("^[가-힣a-zA-Z0-9_-]*$")

    fun filterInput(input: String): String =
        input
            .filter { char ->
                char.toString().matches(nicknameRegex)
            }.take(MAX_LENGTH)

    fun validate(nickname: String): NicknameValidationResult {
        if (nickname.isEmpty()) return NicknameValidationResult.Empty
        if (nickname.length < MIN_LENGTH) return NicknameValidationResult.TooShort
        if (nickname.length > MAX_LENGTH) return NicknameValidationResult.TooLong
        if (!nickname.matches(nicknameRegex)) return NicknameValidationResult.InvalidCharacter
        return NicknameValidationResult.Valid
    }

    companion object {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 20
    }
}

sealed interface NicknameValidationResult {
    data object Valid : NicknameValidationResult

    data object Empty : NicknameValidationResult

    data object TooShort : NicknameValidationResult

    data object TooLong : NicknameValidationResult

    data object InvalidCharacter : NicknameValidationResult

    data object Duplicate : NicknameValidationResult

    data object Checking : NicknameValidationResult

    val isValid: Boolean
        get() = this is Valid
}
