package com.whatever.caro.feature.profile.create.mvi

import com.whatever.caro.core.viewmodel.contract.UiState
import com.whatever.caro.feature.profile.NicknameValidationResult
import com.whatever.caro.feature.profile.NicknameValidator

data class CreateProfileState(
    val nickname: String = "",
    val validationResult: NicknameValidationResult = NicknameValidationResult.Empty,
    val isLoading: Boolean = false,
    val isRandomNicknameLoading: Boolean = false,
    val isCancelDialogVisible: Boolean = false,
) : UiState {
    val characterCount: String
        get() = "${nickname.length}/${NicknameValidator.MAX_LENGTH}"

    val isConfirmEnabled: Boolean
        get() = validationResult.isValid && isLoading.not()
}
