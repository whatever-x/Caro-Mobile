package com.whatever.caro.feature.profile.mvi

import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.profile.usecase.NicknameValidationResult
import com.whatever.caro.feature.profile.usecase.ValidateNicknameUseCase

data class CreateProfileState(
    val nickname: String = "",
    val placeholder: String = "자동 생성된 닉네임",
    val validationResult: NicknameValidationResult = NicknameValidationResult.Empty,
    val isLoading: Boolean = false,
    val isRandomNicknameLoading: Boolean = false,
) : UiState {
    val characterCount: String
        get() = "${nickname.length}/${ValidateNicknameUseCase.MAX_LENGTH}"

    val isConfirmEnabled: Boolean
        get() = validationResult.isValid && isLoading.not()
}
