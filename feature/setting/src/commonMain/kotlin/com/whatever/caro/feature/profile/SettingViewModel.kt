package com.whatever.caro.feature.profile

import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.profile.model.WebViewType
import com.whatever.caro.feature.profile.mvi.SettingIntent
import com.whatever.caro.feature.profile.mvi.SettingSideEffect
import com.whatever.caro.feature.profile.mvi.SettingState

class SettingViewModel(
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<SettingState, SettingIntent, SettingSideEffect>(
        initialState = SettingState(),
        exceptionFilter = exceptionFilter,
    ) {
    override fun handleClientException(throwable: Throwable) {
        super.handleClientException(throwable)
    }

    override suspend fun handleIntent(intent: SettingIntent) {
        when (intent) {
            SettingIntent.ClickDeleteAccount -> {
                TODO()
            }

            SettingIntent.ClickLogOut -> {
                logout()
            }

            SettingIntent.ClickNicknameChange -> {
                postSideEffect(
                    SettingSideEffect.NavigateToEditNickName(
                        currentNickname = currentState.nickname,
                    ),
                )
            }

            SettingIntent.ClickPrivacyPolicy -> {
                postSideEffect(
                    SettingSideEffect.NavigateWebView(
                        type = WebViewType.PRIVACY_POLICY,
                    ),
                )
            }

            SettingIntent.ClickReportBug -> {
                postSideEffect(SettingSideEffect.NavigateWebView(type = WebViewType.REPORT_BUG))
            }

            SettingIntent.ClickTermsOfService -> {
                postSideEffect(
                    SettingSideEffect.NavigateWebView(
                        type = WebViewType.TERMS_OF_SERVICE,
                    ),
                )
            }
        }
    }

    private fun logout() {
    }
}
