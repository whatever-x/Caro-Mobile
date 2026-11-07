package com.whatever.caro.feature.setting.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.withResumed
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.setting_dialog_button_cancel
import caromobile.core.designsystem.generated.resources.setting_dialog_button_delete_account
import caromobile.core.designsystem.generated.resources.setting_dialog_content
import caromobile.core.designsystem.generated.resources.setting_dialog_title
import caromobile.core.designsystem.generated.resources.setting_notification_error
import caromobile.core.designsystem.generated.resources.setting_notification_open_settings
import caromobile.core.designsystem.generated.resources.setting_notification_permission_content
import caromobile.core.designsystem.generated.resources.setting_notification_permission_title
import caromobile.core.designsystem.generated.resources.setting_privcay_policy_url
import caromobile.core.designsystem.generated.resources.setting_report_bug_url
import caromobile.core.designsystem.generated.resources.setting_snackbar_delete_account
import caromobile.core.designsystem.generated.resources.setting_snackbar_delete_account_error
import caromobile.core.designsystem.generated.resources.setting_snackbar_logout
import caromobile.core.designsystem.generated.resources.setting_snackbar_user_info_load_error
import caromobile.core.designsystem.generated.resources.setting_terms_of_service_url
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.components.CaroDialogButton
import com.whatever.caro.core.designsystem.components.CaroSnackbarStyle
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.inappreview.InAppReviewRequester
import com.whatever.caro.core.inappreview.rememberInAppReviewRequester
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.EditProfileEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.core.ui.notification.rememberNotificationPermissionController
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.setting.SettingScreen
import com.whatever.caro.feature.setting.SettingViewModel
import com.whatever.caro.feature.setting.model.AppConfig
import com.whatever.caro.feature.setting.model.SnackbarType
import com.whatever.caro.feature.setting.model.WebViewType
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingSideEffect
import com.whatever.caro.feature.setting.platform.InquiryLifecycleEffect
import com.whatever.caro.feature.setting.platform.rememberOpenInquiry
import com.whatever.caro.feature.setting.platform.rememberShouldPreserveInquiryOnDispose
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingRoute(
    viewModel: SettingViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
    inquiryOpener: suspend (String) -> Boolean = rememberOpenInquiry(),
    inAppReviewRequester: InAppReviewRequester = rememberInAppReviewRequester(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val uriHandler = LocalUriHandler.current
    val permissionController by rememberUpdatedState(rememberNotificationPermissionController())
    val scope = rememberCoroutineScope()
    val notificationErrorMessage = stringResource(Res.string.setting_notification_error)
    val reviewRequester by rememberUpdatedState(inAppReviewRequester)
    val openInquiry by rememberUpdatedState(inquiryOpener)
    val shouldPreserveInquiryOnDispose by rememberUpdatedState(rememberShouldPreserveInquiryOnDispose())
    val routeLifecycle = LocalLifecycleOwner.current.lifecycle
    val privacyPolicyUrl = stringResource(resource = Res.string.setting_privcay_policy_url)
    val termsOfServiceUrl = stringResource(resource = Res.string.setting_terms_of_service_url)
    val reportBugUrl by rememberUpdatedState(stringResource(resource = Res.string.setting_report_bug_url))
    val logoutSnackbarMessage = stringResource(resource = Res.string.setting_snackbar_logout)
    val deleteAccountSnackbarMessage =
        stringResource(resource = Res.string.setting_snackbar_delete_account)
    val deleteAccountErrorSnackbarMessage =
        stringResource(resource = Res.string.setting_snackbar_delete_account_error)
    val userInfoLoadErrorSnackbarMessage =
        stringResource(resource = Res.string.setting_snackbar_user_info_load_error)

    InquiryLifecycleEffect(
        onBackground = { viewModel.intent(SettingIntent.AppBackgrounded) },
        onForeground = { viewModel.intent(SettingIntent.AppResumed) },
    )

    NavigationBackHandler(
        state = backState,
        onBackCompleted = {
            if (!state.isLoading && !state.isNotificationLoading) {
                viewModel.intent(SettingIntent.ClickBack)
            }
        },
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.intent(SettingIntent.Initialize)
        scope.launch {
            try {
                viewModel.intent(SettingIntent.NotificationPermissionRefreshed(permissionController.isGranted()))
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                viewModel.intent(SettingIntent.NotificationPermissionFailed)
            }
        }
    }

    LaunchedEffect(viewModel, routeLifecycle) {
        var openingJob: Job? = null
        var reviewJob: Job? = null
        val reviewObserver =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_PAUSE) reviewJob?.cancel()
            }
        routeLifecycle.addObserver(reviewObserver)
        try {
            viewModel.sideEffect.collect { sideEffect ->
                when (sideEffect) {
                    SettingSideEffect.RequestNotificationPermission -> {
                        scope.launch {
                            try {
                                viewModel.intent(SettingIntent.NotificationPermissionResult(permissionController.request()))
                            } catch (exception: CancellationException) {
                                viewModel.intent(SettingIntent.DismissNotificationPermissionDialog)
                                throw exception
                            } catch (exception: Exception) {
                                viewModel.intent(SettingIntent.NotificationPermissionFailed)
                            }
                        }
                    }

                    SettingSideEffect.OpenNotificationSettings -> {
                        try {
                            permissionController.openSettings()
                        } catch (_: Exception) {
                            viewModel.intent(SettingIntent.NotificationPermissionFailed)
                        }
                    }

                    is SettingSideEffect.NavigateToEditNickName -> {
                        navDispatcher.emit(
                            NavCommand.To(
                                key = EditProfileEntry(nickname = sideEffect.currentNickname),
                            ),
                        )
                    }

                    SettingSideEffect.NavigateToLogin -> {
                        navDispatcher.emit(
                            NavCommand.ResetTo(
                                key = LoginEntry,
                            ),
                        )
                    }

                    is SettingSideEffect.NavigateWebView -> {
                        val url =
                            when (sideEffect.type) {
                                WebViewType.TERMS_OF_SERVICE -> termsOfServiceUrl
                                WebViewType.PRIVACY_POLICY -> privacyPolicyUrl
                                WebViewType.REPORT_BUG -> reportBugUrl
                            }
                        uriHandler.openUri(url)
                    }

                    is SettingSideEffect.OpenInquiry -> {
                        if (!viewModel.isCurrentInquiryRequest(sideEffect.requestId)) return@collect
                        openingJob?.cancel()
                        reviewJob?.cancel()
                        openingJob =
                            launch {
                                try {
                                    val opened = openInquiry(reportBugUrl)
                                    viewModel.intent(SettingIntent.InquiryOpenResult(sideEffect.requestId, opened))
                                } catch (cancellation: CancellationException) {
                                    viewModel.intent(SettingIntent.InquiryOpenCancelled(sideEffect.requestId))
                                    throw cancellation
                                } catch (exception: Exception) {
                                    Napier.w(throwable = exception) { "Could not open the inquiry form" }
                                    viewModel.intent(SettingIntent.InquiryOpenResult(sideEffect.requestId, false))
                                }
                            }
                    }

                    is SettingSideEffect.RequestStoreReview -> {
                        reviewJob?.cancel()
                        reviewJob =
                            launch {
                                routeLifecycle.withResumed { }
                                if (viewModel.isCurrentInquiryRequest(sideEffect.requestId)) {
                                    reviewRequester.requestReview()
                                }
                            }
                    }

                    SettingSideEffect.PopBackStack -> {
                        navDispatcher.emit(
                            NavCommand.Back,
                        )
                    }

                    is SettingSideEffect.ShowSnackbar -> {
                        val message =
                            when (sideEffect.type) {
                                SnackbarType.LOGOUT -> logoutSnackbarMessage
                                SnackbarType.DELETE_ACCOUNT -> deleteAccountSnackbarMessage
                                SnackbarType.DELETE_ACCOUNT_ERROR -> deleteAccountErrorSnackbarMessage
                                SnackbarType.USER_INFO_LOAD_ERROR -> userInfoLoadErrorSnackbarMessage
                                SnackbarType.NOTIFICATION_CHANGE_ERROR -> notificationErrorMessage
                            }
                        val style =
                            when (sideEffect.type) {
                                SnackbarType.NOTIFICATION_CHANGE_ERROR,
                                SnackbarType.DELETE_ACCOUNT_ERROR,
                                SnackbarType.USER_INFO_LOAD_ERROR,
                                -> CaroSnackbarStyle.Error

                                SnackbarType.LOGOUT,
                                SnackbarType.DELETE_ACCOUNT,
                                -> CaroSnackbarStyle.Normal
                            }
                        snackbarController.show(SnackBarMessage(message = message, style = style))
                    }
                }
            }
        } finally {
            routeLifecycle.removeObserver(reviewObserver)
            openingJob?.cancel()
            reviewJob?.cancel()
            if (!shouldPreserveInquiryOnDispose()) viewModel.intent(SettingIntent.CancelPendingInquiry)
        }
    }

    SettingScreen(
        state = state,
        appVersion = AppConfig.appVersion,
        onIntent = viewModel::intent,
    )
    if (state.notificationPermissionDialogVisible) {
        NotificationPermissionDialog(
            onDismiss = { viewModel.intent(SettingIntent.DismissNotificationPermissionDialog) },
            onOpenSettings = { viewModel.intent(SettingIntent.OpenNotificationSettings) },
        )
    }
    if (state.accountDeleteDialogVisible) {
        DeleteAccountDialog(
            enabled = !state.isDeletingAccount,
            onDeleteAccountClick = { viewModel.intent(SettingIntent.ClickDeleteAccountDialogConfirm) },
            onCancelClick = { viewModel.intent(SettingIntent.ClickDeleteAccountDialogCancel) },
        )
    }
}

@Composable
private fun DeleteAccountDialog(
    enabled: Boolean,
    onDeleteAccountClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    CaroDialog(
        onDismissRequest = {},
        title = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.setting_dialog_title),
                color = CaroTheme.color.text.primary,
                style = CaroTheme.typography.heading2,
            )
        },
        content = {
            Spacer(modifier = Modifier.size(size = CaroTheme.spacing.s))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.setting_dialog_content),
                color = CaroTheme.color.text.secondary,
                style = CaroTheme.typography.body2.medium,
            )
            Spacer(modifier = Modifier.size(size = CaroTheme.spacing.m))
        },
        buttons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            ) {
                DialogButton(
                    text = stringResource(Res.string.setting_dialog_button_delete_account),
                    enabled = enabled,
                    backgroundColor = CaroTheme.color.surface.dangerous,
                    contentColor = CaroTheme.color.text.dangerous,
                    onClick = onDeleteAccountClick,
                )
                DialogButton(
                    text = stringResource(Res.string.setting_dialog_button_cancel),
                    enabled = enabled,
                    backgroundColor = CaroTheme.color.surface.tertiary,
                    contentColor = CaroTheme.color.text.brand,
                    onClick = onCancelClick,
                )
            }
        },
    )
}

@Composable
private fun DialogButton(
    text: String,
    enabled: Boolean,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Text(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = backgroundColor,
                    shape = CaroTheme.shape.xxl,
                ).padding(
                    horizontal = CaroTheme.spacing.l,
                    vertical = CaroTheme.spacing.m,
                ).noRippleClickable(enabled = enabled, onClick = onClick),
        text = text,
        color = if (enabled) contentColor else CaroTheme.color.text.disabled,
        style = CaroTheme.typography.caption1.regular,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    CaroDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.setting_notification_permission_title),
                style = CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
            )
        },
        content = {
            Spacer(Modifier.size(CaroTheme.spacing.s))
            Text(
                text = stringResource(Res.string.setting_notification_permission_content),
                textAlign = TextAlign.Center,
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
            )
            Spacer(Modifier.size(CaroTheme.spacing.m))
        },
        buttons = {
            Row(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
                CaroDialogButton(
                    text = stringResource(Res.string.setting_dialog_button_cancel),
                    backgroundColor = CaroTheme.color.surface.tertiary,
                    textColor = CaroTheme.color.text.brand,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                CaroDialogButton(
                    text = stringResource(Res.string.setting_notification_open_settings),
                    backgroundColor = CaroTheme.color.surface.brand,
                    textColor = CaroTheme.color.text.inverse,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenSettings,
                )
            }
        },
    )
}
