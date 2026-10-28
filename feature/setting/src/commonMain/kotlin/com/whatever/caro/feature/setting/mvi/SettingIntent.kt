package com.whatever.caro.feature.setting.mvi

import com.whatever.caro.core.viewmodel.contract.UiIntent

sealed interface SettingIntent : UiIntent {
    data object ClickNotificationToggle : SettingIntent

    data class NotificationPermissionResult(
        val granted: Boolean,
    ) : SettingIntent

    data class NotificationPermissionRefreshed(
        val granted: Boolean,
    ) : SettingIntent

    data object DismissNotificationPermissionDialog : SettingIntent

    data object OpenNotificationSettings : SettingIntent

    data object NotificationPermissionFailed : SettingIntent

    data class InquiryOpenResult(
        val requestId: Long,
        val opened: Boolean,
    ) : SettingIntent

    data class InquiryOpenCancelled(
        val requestId: Long,
    ) : SettingIntent

    data object CancelPendingInquiry : SettingIntent

    data object AppBackgrounded : SettingIntent

    data object AppResumed : SettingIntent

    data object Initialize : SettingIntent

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
