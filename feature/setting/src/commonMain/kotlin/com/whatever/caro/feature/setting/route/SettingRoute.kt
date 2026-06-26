package com.whatever.caro.feature.setting.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.setting_dialog_button_cancel
import caromobile.core.designsystem.generated.resources.setting_dialog_button_delete_account
import caromobile.core.designsystem.generated.resources.setting_dialog_content
import caromobile.core.designsystem.generated.resources.setting_dialog_title
import caromobile.core.designsystem.generated.resources.setting_privcay_policy_url
import caromobile.core.designsystem.generated.resources.setting_report_bug_url
import caromobile.core.designsystem.generated.resources.setting_snackbar_delete_account
import caromobile.core.designsystem.generated.resources.setting_snackbar_logout
import caromobile.core.designsystem.generated.resources.setting_terms_of_service_url
import com.whatever.caro.core.designsystem.components.CaroDialog
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.EditProfileEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.core.ui.modifier.noRippleClickable
import com.whatever.caro.core.ui.snackbar.SnackBarMessage
import com.whatever.caro.core.ui.snackbar.SnackbarController
import com.whatever.caro.feature.setting.SettingScreen
import com.whatever.caro.feature.setting.SettingViewModel
import com.whatever.caro.feature.setting.model.SnackbarType
import com.whatever.caro.feature.setting.model.WebViewType
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingRoute(
    viewModel: SettingViewModel,
    navDispatcher: NavigationDispatcher,
    snackbarController: SnackbarController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val privacyPolicyUrl = stringResource(resource = Res.string.setting_privcay_policy_url)
    val termsOfServiceUrl = stringResource(resource = Res.string.setting_terms_of_service_url)
    val reportBugUrl = stringResource(resource = Res.string.setting_report_bug_url)
    val logoutSnackbarMessage = stringResource(resource = Res.string.setting_snackbar_logout)
    val deleteAccountSnackbarMessage =
        stringResource(resource = Res.string.setting_snackbar_delete_account)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.intent(SettingIntent.Initialize)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
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
                        }
                    snackbarController.show(SnackBarMessage(message = message))
                }
            }
        }
    }

    SettingScreen(
        state = state,
        onIntent = viewModel::intent,
    )
    if (state.accountDeleteDialogVisible) {
        DeleteAccountDialog(
            onDeleteAccountClick = { viewModel.intent(SettingIntent.ClickDeleteAccountDialogConfirm) },
            onCancelClick = { viewModel.intent(SettingIntent.ClickDeleteAccountDialogCancel) },
        )
    }
}

@Composable
private fun DeleteAccountDialog(
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
                style = CaroTheme.typography.body3,
            )
            Spacer(modifier = Modifier.size(size = CaroTheme.spacing.m))
        },
        buttons = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            ) {
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = CaroTheme.color.badge.surface.error,
                                shape = CaroTheme.shape.xxl,
                            ).padding(
                                horizontal = CaroTheme.spacing.l,
                                vertical = CaroTheme.spacing.m,
                            ).noRippleClickable(onDeleteAccountClick),
                    text = stringResource(Res.string.setting_dialog_button_delete_account),
                    color = Color(0xFFFF7A70),
                    style = CaroTheme.typography.caption1,
                    textAlign = TextAlign.Center,
                )
                Text(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = CaroTheme.color.surface.tertiary,
                                shape = CaroTheme.shape.xxl,
                            ).padding(
                                horizontal = CaroTheme.spacing.l,
                                vertical = CaroTheme.spacing.m,
                            ).noRippleClickable(onCancelClick),
                    text = stringResource(Res.string.setting_dialog_button_cancel),
                    color = CaroTheme.color.text.brand,
                    style = CaroTheme.typography.caption1,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}
