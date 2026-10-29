package com.whatever.caro.feature.setting

import com.whatever.caro.core.ads.InterstitialAdController
import com.whatever.caro.core.data.repository.auth.AuthRepository
import com.whatever.caro.core.data.repository.fcm.FcmTokenRepository
import com.whatever.caro.core.data.repository.profile.ProfileRepository
import com.whatever.caro.core.data.util.suspendRunCatching
import com.whatever.caro.core.viewmodel.BaseViewModel
import com.whatever.caro.core.viewmodel.ExceptionFilter
import com.whatever.caro.feature.setting.model.SnackbarType
import com.whatever.caro.feature.setting.model.WebViewType
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingSideEffect
import com.whatever.caro.feature.setting.mvi.SettingState
import kotlinx.coroutines.CancellationException

class SettingViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    exceptionFilter: ExceptionFilter,
    private val fcmTokenRepository: FcmTokenRepository,
    private val adsController: InterstitialAdController,
) : BaseViewModel<SettingState, SettingIntent, SettingSideEffect>(
        initialState = SettingState(),
        exceptionFilter = exceptionFilter,
    ) {
    private var initializeGeneration = 0L
    private var pendingNotificationOn = false
    private var waitingForNotificationSettings = false

    init {
        launch {
            adsController.privacyOptionsRequired.collect { required ->
                reduce { copy(adPrivacyOptionsRequired = required) }
            }
        }
        launch {
            suspendRunCatching {
                fcmTokenRepository.observeNotificationEnabled().collect { enabled ->
                    reduce { copy(notificationEnabled = enabled) }
                }
            }.onFailure { notificationError() }
        }
    }

    override suspend fun handleIntent(intent: SettingIntent) {
        when (intent) {
            SettingIntent.ClickNotificationToggle -> {
                if (currentState.isNotificationLoading || currentState.isDeletingAccount) return
                reduce { copy(isNotificationLoading = true) }
                if (currentState.notificationEnabled) {
                    changeNotification(false)
                } else {
                    pendingNotificationOn = true
                    postSideEffect(SettingSideEffect.RequestNotificationPermission)
                }
            }

            is SettingIntent.NotificationPermissionResult -> {
                fcmTokenRepository.updateNotificationPermission(intent.granted)
                if (!pendingNotificationOn || waitingForNotificationSettings) return
                if (intent.granted) {
                    pendingNotificationOn = false
                    changeNotification(true)
                } else {
                    reduce { copy(isNotificationLoading = false, notificationPermissionDialogVisible = true) }
                }
            }

            is SettingIntent.NotificationPermissionRefreshed -> {
                fcmTokenRepository.updateNotificationPermission(intent.granted)
                if (waitingForNotificationSettings) {
                    waitingForNotificationSettings = false
                    pendingNotificationOn = false
                    if (intent.granted) changeNotification(true) else reduce { copy(isNotificationLoading = false) }
                }
            }

            SettingIntent.DismissNotificationPermissionDialog -> {
                pendingNotificationOn = false
                waitingForNotificationSettings = false
                reduce { copy(notificationPermissionDialogVisible = false, isNotificationLoading = false) }
            }

            SettingIntent.OpenNotificationSettings -> {
                if (!pendingNotificationOn || waitingForNotificationSettings) return
                waitingForNotificationSettings = true
                reduce { copy(notificationPermissionDialogVisible = false, isNotificationLoading = true) }
                postSideEffect(SettingSideEffect.OpenNotificationSettings)
            }

            SettingIntent.NotificationPermissionFailed -> {
                pendingNotificationOn = false
                waitingForNotificationSettings = false
                reduce { copy(isNotificationLoading = false, notificationPermissionDialogVisible = false) }
                notificationError()
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

            SettingIntent.ClickAdPrivacyOptions -> {
                adsController.showPrivacyOptions()
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

    private suspend fun changeNotification(enabled: Boolean) {
        try {
            fcmTokenRepository.setNotificationEnabled(enabled)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            notificationError()
        } finally {
            reduce { copy(isNotificationLoading = false) }
        }
    }

    private fun notificationError() {
        postSideEffect(SettingSideEffect.ShowSnackbar(SnackbarType.NOTIFICATION_CHANGE_ERROR))
    }

    private suspend fun initialize() {
        val generation = ++initializeGeneration
        reduce { copy(isLoading = true) }
        suspendRunCatching {
            profileRepository.getMyInfo()
        }.onSuccess { myInfo ->
            if (generation != initializeGeneration) return
            reduce {
                copy(
                    isLoading = false,
                    nickname = myInfo.nickname,
                    emailAddress = myInfo.email,
                    socialLoginType = myInfo.socialLoginType,
                )
            }
        }.onFailure {
            if (generation != initializeGeneration) return
            reduce { copy(isLoading = false) }
            postSideEffect(SettingSideEffect.ShowSnackbar(type = SnackbarType.USER_INFO_LOAD_ERROR))
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
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (_: Throwable) {
            postSideEffect(SettingSideEffect.ShowSnackbar(type = SnackbarType.DELETE_ACCOUNT_ERROR))
        } finally {
            reduce { copy(isDeletingAccount = false) }
        }
    }

    private suspend fun logout() {
        if (currentState.isLoading || currentState.isNotificationLoading) return
        reduce { copy(isLoading = true) }
        try {
            authRepository.logout()
            postSideEffect(SettingSideEffect.ShowSnackbar(type = SnackbarType.LOGOUT))
            postSideEffect(SettingSideEffect.NavigateToLogin)
        } finally {
            reduce { copy(isLoading = false) }
        }
    }
}
