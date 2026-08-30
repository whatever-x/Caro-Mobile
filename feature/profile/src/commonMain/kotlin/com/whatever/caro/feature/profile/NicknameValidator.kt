package com.whatever.caro.feature.profile

class NicknameValidator {
    private val nicknameRegex = Regex("^[가-힣a-zA-Z0-9_-]*$")

    // 한글 IME는 조합 중 낱자(ㄱ, ㅏ)를 호환 자모로 보낸다. 입력 단계에서 걸러내면
    // 조합이 매번 깨져 한글을 아예 입력할 수 없으므로 낱자까지 허용한다.
    // 완성되지 않은 낱자는 validate() 에서 InvalidCharacter 로 걸러진다.
    private val inputRegex = Regex("^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9_-]*$")

    fun filterInput(input: String): String =
        input
            .filter { char ->
                char.toString().matches(inputRegex)
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
