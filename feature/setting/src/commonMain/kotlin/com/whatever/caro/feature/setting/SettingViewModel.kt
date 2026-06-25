package com.whatever.caro.feature.setting

import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.setting.model.WebViewType
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingSideEffect
import com.whatever.caro.feature.setting.mvi.SettingState

class SettingViewModel(
    private val authRepository: AuthRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<SettingState, SettingIntent, SettingSideEffect>(
    initialState = SettingState(),
    exceptionFilter = exceptionFilter,
) {
    override suspend fun handleIntent(intent: SettingIntent) {
        when (intent) {
            SettingIntent.ClickDeleteAccount -> {
                TODO()
            }

            SettingIntent.ClickLogOut -> logout()
            SettingIntent.ClickNicknameChange -> postSideEffect(
                SettingSideEffect.NavigateToEditNickName(
                    currentNickname = currentState.nickname,
                ),
            )

            SettingIntent.ClickPrivacyPolicy -> postSideEffect(
                SettingSideEffect.NavigateWebView(
                    type = WebViewType.PRIVACY_POLICY,
                ),
            )

            SettingIntent.ClickReportBug -> postSideEffect(SettingSideEffect.NavigateWebView(type = WebViewType.REPORT_BUG))
            SettingIntent.ClickTermsOfService -> postSideEffect(
                SettingSideEffect.NavigateWebView(
                    type = WebViewType.TERMS_OF_SERVICE,
                ),
            )

        }
    }

    private suspend fun logout() {
        authRepository.logout()
        postSideEffect(SettingSideEffect.NavigateToLogin)
    }
}
