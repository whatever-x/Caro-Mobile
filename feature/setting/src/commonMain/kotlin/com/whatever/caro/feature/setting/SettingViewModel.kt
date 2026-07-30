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
    private var initializeGeneration = 0L

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
        val generation = ++initializeGeneration
        reduce { copy(isLoading = true) }
        try {
            val myInfo = profileRepository.getMyInfo()
            if (generation != initializeGeneration) return
            reduce {
                copy(
                    nickname = myInfo.nickname,
                    emailAddress = myInfo.email,
                    socialLoginType = myInfo.socialLoginType,
                )
            }
        } finally {
            if (generation == initializeGeneration) {
                reduce { copy(isLoading = false) }
            }
        }
    }

    private fun controlAccountDeleteButton() {
        reduce { copy(accountDeleteDialogVisible = !accountDeleteDialogVisible) }
    }

    private suspend fun deleteAccount() {
        if (currentState.isDeletingAccount) return
        reduce { copy(isDeletingAccount = true) }
        try {
            authRepository.withdraw()
            reduce { copy(accountDeleteDialogVisible = false) }
            postSideEffect(SettingSideEffect.ShowSnackbar(type = SnackbarType.DELETE_ACCOUNT))
            postSideEffect(SettingSideEffect.NavigateToLogin)
        } finally {
            reduce { copy(isDeletingAccount = false) }
        }
    }

    private suspend fun logout() {
        authRepository.logout()
        postSideEffect(SettingSideEffect.ShowSnackbar(type = SnackbarType.LOGOUT))
        postSideEffect(SettingSideEffect.NavigateToLogin)
    }
}
