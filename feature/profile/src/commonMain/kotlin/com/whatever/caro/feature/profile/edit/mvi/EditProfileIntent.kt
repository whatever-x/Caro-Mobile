package com.whatever.caro.feature.profile.edit.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface EditProfileIntent : UiIntent {
    data class UpdateNickname(
        val nickname: String,
    ) : EditProfileIntent

    data object ClickRefresh : EditProfileIntent

    data object ClickConfirm : EditProfileIntent

    data object ClickBack : EditProfileIntent
}
