package com.whatever.caro.feature.setting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingState
import com.whatever.caro.feature.setting.route.NotificationPermissionDialog
import com.whatever.caro.screenshot.ScreenshotScenario

internal object SettingScreenshotScenario {
    const val ID = "setting.setting.account"

    fun scenarios(): List<ScreenshotScenario> {
        var clicks = 0
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
                beforeCapture = { language -> onNodeWithText(if (language == "ko") "설정" else "Settings").assertIsDisplayed() },
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
                beforeCapture = { language ->
                    onNodeWithText(if (language == "ko") "알림 수신" else "Notifications").assertIsOn().performClick()
                    check(clicks == 1) { "Notification toggle must emit exactly one action" }
                },
            ),
            ScreenshotScenario(
                "setting.notification.busy",
                render = { CaroTheme { SettingScreen(SettingState(isLoading = false, isNotificationLoading = true), "1.0", {}) } },
                beforeCapture = { language ->
                    onNodeWithText(if (language == "ko") "알림 수신" else "Notifications").assertIsNotEnabled()
                },
            ),
            ScreenshotScenario(
                "setting.notification.small.large-font",
                render = {
                    CaroTheme {
                        SettingScreen(
                            SettingState(isLoading = false, nickname = "현란하게_춤추는_연", emailAddress = "caro@example.com"),
                            "1.0",
                            {},
                        )
                    }
                },
                beforeCapture = { language ->
                    onNodeWithText(if (language == "ko") "계정탈퇴" else "Delete Account").performScrollTo().assertIsDisplayed()
                },
            ),
            ScreenshotScenario(
                "setting.notification.permission",
                render = { CaroTheme { NotificationPermissionDialog({ cancelled++ }, { opened++ }) } },
                isDialog = true,
                beforeCapture = { language ->
                    onNodeWithText(if (language == "ko") "알림 권한이 필요해요" else "Notification permission required").assertIsDisplayed()
                },
                afterCapture = { language ->
                    onNodeWithText(if (language == "ko") "취소" else "Cancel").performClick()
                    onNodeWithText(if (language == "ko") "앱 설정으로" else "App settings").performClick()
                    check(cancelled == 1 && opened == 1) { "Permission dialog callbacks must each run once" }
                },
            ),
        )
    }
}
