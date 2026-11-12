package com.whatever.caro.feature.setting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.setting.mvi.SettingIntent
import com.whatever.caro.feature.setting.mvi.SettingState
import com.whatever.caro.feature.setting.route.NotificationPermissionDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class SettingScreenshotAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun adPrivacyMenuDispatchesOneAction() {
        var clicks = 0
        composeRule.setContent {
            CaroTheme {
                SettingScreen(
                    state = SettingState(isLoading = false),
                    appVersion = "1.0",
                    showAdPrivacyOptions = true,
                    onIntent = { if (it == SettingIntent.ClickAdPrivacyOptions) clicks++ },
                )
            }
        }
        composeRule
            .onNodeWithText("광고 개인정보 설정")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertEquals(1, clicks)
        composeRule.onRoot().captureRoboImage("setting.ad-privacy.required.png")
    }

    @Test
    fun adPrivacyMenuIsAbsentWhenNotRequired() {
        composeRule.setContent {
            CaroTheme { SettingScreen(SettingState(isLoading = false), "1.0", onIntent = {}) }
        }
        composeRule.onNodeWithText("광고 개인정보 설정").assertDoesNotExist()
    }

    @Test
    fun notificationToggleHasStateAndOneAction() {
        var clicks = 0
        composeRule.setContent {
            CaroTheme {
                SettingScreen(SettingState(isLoading = false, notificationEnabled = true), "1.0") { intent ->
                    if (intent == SettingIntent.ClickNotificationToggle) clicks++
                }
            }
        }
        composeRule.onNodeWithText("알림 수신").assertIsOn().performClick()
        assertEquals(1, clicks)
        composeRule.onRoot().captureRoboImage("setting.notification.on.png")
    }

    @Test
    fun notificationBusyBlocksChanges() {
        composeRule.setContent {
            CaroTheme {
                SettingScreen(
                    SettingState(isLoading = false, isNotificationLoading = true),
                    "1.0",
                    onIntent = {},
                )
            }
        }
        composeRule.onNodeWithText("알림 수신").assertIsNotEnabled()
        composeRule.onRoot().captureRoboImage("setting.notification.busy.png")
    }

    @Test
    fun smallScreenWithLargeFontCanReachAccountActions() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                CaroTheme {
                    Box(Modifier.requiredSize(320.dp, 480.dp)) {
                        SettingScreen(
                            SettingState(isLoading = false, nickname = "현란하게_춤추는_연", emailAddress = "caro@example.com"),
                            "1.0",
                            onIntent = {},
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithText("계정탈퇴").performScrollTo().assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("setting.notification.small.large-font.png")
    }

    @Test
    fun permissionGuideOffersCancelAndSettings() {
        var cancelled = 0
        var opened = 0
        composeRule.setContent { CaroTheme { NotificationPermissionDialog({ cancelled++ }, { opened++ }) } }
        composeRule.onNodeWithText("알림 권한이 필요해요").assertIsDisplayed()
        composeRule.onRoot().captureRoboImage("setting.notification.permission.png")
        composeRule.onNodeWithText("취소").performClick()
        composeRule.onNodeWithText("앱 설정으로").performClick()
        assertEquals(1, cancelled)
        assertEquals(1, opened)
    }

    @Test
    fun initial() {
        composeRule.setContent { SettingScreenshotScenario.Render() }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(SettingScreenshotScenario.FILE_NAME)
    }
}
