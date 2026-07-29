package com.whatever.caro.feature.setting

import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.setting.model.SnackbarType
import com.whatever.caro.feature.setting.model.WebViewType
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingSideEffect
import com.whatever.caro.feature.setting.mvi.SettingState

class SettingViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    exceptionFilter: ExceptionFilter,
) : BaseViewModel<SettingState, SettingIntent, SettingSideEffect>(
        initialState = SettingState(),
        exceptionFilter = exceptionFilter,
    ) {
    private var isInitializing = false

    override fun handleClientException(throwable: Throwable) {
        reduce { copy(isLoading = false) }
    }

    override suspend fun handleIntent(intent: SettingIntent) {
        when (intent) {
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

            SettingIntent.ClickDeleteAccountDialogCancel,
            SettingIntent.ClickDeleteAccount,
            -> {
                controlAccountDeleteButton()
            }

            SettingIntent.ClickDeleteAccountDialogConfirm -> {
                deleteAccount()
            }

            SettingIntent.Initialize -> {
                initialize()
            }
        }
    }

    private suspend fun initialize() {
        if (isInitializing) return
        isInitializing = true
        reduce { copy(isLoading = true) }
        try {
            val nickname = profileRepository.getMyNickname()
            reduce { copy(isLoading = false, nickname = nickname) }
        } catch (throwable: Throwable) {
            reduce { copy(isLoading = false) }
            throw throwable
        } finally {
            isInitializing = false
        }
    }

    private fun controlAccountDeleteButton() {
        reduce { copy(accountDeleteDialogVisible = !accountDeleteDialogVisible) }
    }

    // TODO: 계정 삭제 필요
    private fun deleteAccount() {
        reduce { copy(accountDeleteDialogVisible = false) }
        postSideEffect(SettingSideEffect.ShowSnackbar(type = SnackbarType.DELETE_ACCOUNT))
        postSideEffect(SettingSideEffect.NavigateToLogin)
    }

    private suspend fun logout() {
        authRepository.logout()
        postSideEffect(SettingSideEffect.ShowSnackbar(type = SnackbarType.LOGOUT))
        postSideEffect(SettingSideEffect.NavigateToLogin)
    }
}
