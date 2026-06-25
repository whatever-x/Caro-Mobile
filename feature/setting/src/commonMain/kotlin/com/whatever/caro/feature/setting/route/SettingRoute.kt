package com.whatever.caro.feature.setting.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.setting_privcay_policy_url
import caromobile.core.designsystem.generated.resources.setting_report_bug_url
import caromobile.core.designsystem.generated.resources.setting_terms_of_service_url
import com.whatever.caro.core.navigator.contract.NavCommand
import com.whatever.caro.core.navigator.dispatcher.NavigationDispatcher
import com.whatever.caro.core.navigator.entries.EditProfileEntry
import com.whatever.caro.core.navigator.entries.LoginEntry
import com.whatever.caro.feature.setting.SettingScreen
import com.whatever.caro.feature.setting.SettingViewModel
import com.whatever.caro.feature.setting.model.WebViewType
import com.whatever.caro.feature.setting.mvi.SettingSideEffect
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingRoute(
    viewModel: SettingViewModel,
    navDispatcher: NavigationDispatcher,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val privacyPolicyUrl = stringResource(resource = Res.string.setting_privcay_policy_url)
    val termsOfServiceUrl = stringResource(resource = Res.string.setting_terms_of_service_url)
    val reportBugUrl = stringResource(resource = Res.string.setting_report_bug_url)

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is SettingSideEffect.NavigateToEditNickName -> {
                    navDispatcher.emit(
                        NavCommand.To(
                            key = EditProfileEntry(nickname = state.nickname),
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
            }
        }
    }

    SettingScreen(
        state = state,
        onIntent = viewModel::intent,
    )
}
