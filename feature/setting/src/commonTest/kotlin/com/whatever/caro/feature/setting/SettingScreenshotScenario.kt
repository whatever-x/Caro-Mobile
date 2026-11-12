package com.whatever.caro.feature.setting

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.setting_dialog_button_cancel
import caromobile.core.designsystem.generated.resources.setting_menu_ad_privacy_options
import caromobile.core.designsystem.generated.resources.setting_menu_delete_account
import caromobile.core.designsystem.generated.resources.setting_notification_label
import caromobile.core.designsystem.generated.resources.setting_notification_open_settings
import caromobile.core.designsystem.generated.resources.setting_notification_permission_title
import caromobile.core.designsystem.generated.resources.setting_title
import com.whatever.caro.core.designsystem.components.CARO_DIALOG_CONTENT_TAG
import com.whatever.caro.core.designsystem.components.LocalCaroDialogContentOnly
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.core.screenshot.testing.ScreenshotScenario
import com.whatever.caro.core.screenshot.testing.ScreenshotString
import com.whatever.caro.core.screenshot.testing.ScreenshotTarget
import com.whatever.caro.core.screenshot.testing.ScreenshotVariant
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingState
import com.whatever.caro.feature.setting.route.NotificationPermissionDialog

internal object SettingScreenshotScenario {
    const val ID = "setting.setting.account"

    fun scenarios(): List<ScreenshotScenario> {
        var clicks = 0
        var adPrivacyClicks = 0
        var cancelled = 0
        var opened = 0
        return listOf(
            ScreenshotScenario(
                ID,
                render = {
                    CaroTheme {
                        SettingScreen(
                            state =
                                SettingState(
                                    isLoading = false,
                                    nickname = "승우",
                                    emailAddress = "caro@example.com",
                                    socialLoginType = SocialLoginType.GOOGLE,
                                ),
                            appVersion = "1.0.0",
                            onIntent = {},
                        )
                    }
                },
                expectedStrings =
                    listOf(
                        ScreenshotString(Res.string.setting_title),
                        ScreenshotString(Res.string.setting_menu_ad_privacy_options),
                    ),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.setting_title]).assertIsDisplayed()
                    onNodeWithText(strings[Res.string.setting_menu_ad_privacy_options]).assertDoesNotExist()
                },
            ),
            ScreenshotScenario(
                "setting.ad-privacy.required",
                render = {
                    CaroTheme {
                        SettingScreen(
                            state = SettingState(isLoading = false),
                            appVersion = "1.0",
                            showAdPrivacyOptions = true,
                            onIntent = { if (it == SettingIntent.ClickAdPrivacyOptions) adPrivacyClicks++ },
                        )
                    }
                },
                expectedStrings = listOf(ScreenshotString(Res.string.setting_menu_ad_privacy_options)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.setting_menu_ad_privacy_options])
                        .performScrollTo()
                        .assertIsDisplayed()
                        .performClick()
                    check(adPrivacyClicks == 1) { "Ad privacy menu must emit exactly one action" }
                },
            ),
            ScreenshotScenario(
                "setting.notification.on",
                render = {
                    CaroTheme {
                        SettingScreen(SettingState(isLoading = false, notificationEnabled = true), "1.0") { intent ->
                            if (intent == SettingIntent.ClickNotificationToggle) clicks++
                        }
                    }
                },
                expectedStrings = listOf(ScreenshotString(Res.string.setting_notification_label)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.setting_notification_label]).assertIsOn().performClick()
                    check(clicks == 1) { "Notification toggle must emit exactly one action" }
                },
            ),
            ScreenshotScenario(
                "setting.notification.busy",
                render = {
                    CaroTheme {
                        SettingScreen(
                            SettingState(isLoading = false, isNotificationLoading = true),
                            "1.0",
                            onIntent = {},
                        )
                    }
                },
                expectedStrings = listOf(ScreenshotString(Res.string.setting_notification_label)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.setting_notification_label]).assertIsNotEnabled()
                },
            ),
            ScreenshotScenario(
                "setting.notification.small.large-font",
                variant = ScreenshotVariant.SMALL_LARGE_FONT,
                render = {
                    CaroTheme {
                        SettingScreen(
                            SettingState(isLoading = false, nickname = "현란하게_춤추는_연", emailAddress = "caro@example.com"),
                            "1.0",
                            onIntent = {},
                        )
                    }
                },
                expectedStrings = listOf(ScreenshotString(Res.string.setting_menu_delete_account)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.setting_menu_delete_account]).performScrollTo().assertIsDisplayed()
                },
            ),
            ScreenshotScenario(
                "setting.notification.permission",
                render = { CaroTheme { NotificationPermissionDialog({ cancelled++ }, { opened++ }) } },
                target = ScreenshotTarget.DIALOG,
                iosDialogContentTag = CARO_DIALOG_CONTENT_TAG,
                iosDialogContent = {
                    CompositionLocalProvider(LocalCaroDialogContentOnly provides true) {
                        CaroTheme { NotificationPermissionDialog({ cancelled++ }, { opened++ }) }
                    }
                },
                expectedStrings =
                    listOf(
                        ScreenshotString(Res.string.setting_notification_permission_title),
                        ScreenshotString(Res.string.setting_dialog_button_cancel),
                        ScreenshotString(Res.string.setting_notification_open_settings),
                    ),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.setting_notification_permission_title]).assertIsDisplayed()
                },
                afterCapture = { strings ->
                    onNodeWithText(strings[Res.string.setting_dialog_button_cancel]).performClick()
                    onNodeWithText(strings[Res.string.setting_notification_open_settings]).performClick()
                    check(cancelled == 1 && opened == 1) { "Permission dialog callbacks must each run once" }
                },
            ),
        )
    }
}
