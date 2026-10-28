package com.whatever.caro.feature.profile.create.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface CreateProfileIntent : UiIntent {
    data class UpdateNickname(
        val nickname: String,
    ) : CreateProfileIntent

    data class UpdateYear(
        val value: String,
    ) : CreateProfileIntent

    data class UpdateMonth(
        val value: String,
    ) : CreateProfileIntent

    data class UpdateDay(
        val value: String,
    ) : CreateProfileIntent

    data object RetryNicknameCheck : CreateProfileIntent

    data object ClickRefresh : CreateProfileIntent

    data object ClickConfirm : CreateProfileIntent

    data object ClickRelogin : CreateProfileIntent

    data object ConfirmRelogin : CreateProfileIntent

    data object DismissRelogin : CreateProfileIntent

    data object ClickBack : CreateProfileIntent

    data object ConfirmCancel : CreateProfileIntent

    data object DismissCancelDialog : CreateProfileIntent
}
