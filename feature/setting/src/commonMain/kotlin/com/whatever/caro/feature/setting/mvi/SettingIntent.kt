package com.whatever.caro.feature.setting.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface SettingIntent : UiIntent {
    data object ClickNicknameChange : SettingIntent

    data object ClickTermsOfService : SettingIntent

    data object ClickBack : SettingIntent

    data object ClickPrivacyPolicy : SettingIntent

    data object ClickReportBug : SettingIntent

    data object ClickLogOut : SettingIntent

    data object ClickDeleteAccount : SettingIntent

    data object ClickDeleteAccountDialogConfirm : SettingIntent

    data object ClickDeleteAccountDialogCancel : SettingIntent
}
