package com.whatever.caro.feature.profile.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface CreateProfileIntent : UiIntent {
    data class UpdateNickname(
        val nickname: String,
    ) : CreateProfileIntent

    data object ClickRefresh : CreateProfileIntent

    data object ClickConfirm : CreateProfileIntent

    data object ClickBack : CreateProfileIntent
}
