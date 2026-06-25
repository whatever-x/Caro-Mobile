package com.whatever.caro.feature.setting

import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.setting.model.ToastType
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
                deleteAccount()
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

            SettingIntent.ClickBack -> {
                postSideEffect(SettingSideEffect.PopBackStack)
            }
        }
    }

    private suspend fun deleteAccount() {
        postSideEffect(SettingSideEffect.ShowToast(type = ToastType.DELETE_ACCOUNT))
    }

    private suspend fun logout() {
        authRepository.logout()
        postSideEffect(SettingSideEffect.ShowToast(type = ToastType.LOGOUT))
        postSideEffect(SettingSideEffect.NavigateToLogin)
    }
}
