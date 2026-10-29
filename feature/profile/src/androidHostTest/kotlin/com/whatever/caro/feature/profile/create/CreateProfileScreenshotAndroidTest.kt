package com.whatever.caro.feature.profile.create

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.profile.NicknameValidationResult
import com.whatever.caro.feature.profile.create.mvi.CreateProfileState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class CreateProfileScreenshotAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun duplicateNicknameShowsError() {
        composeRule.setContent {
            CaroTheme {
                CreateProfileScreen(
                    state = CreateProfileState(nickname = "거북이", validationResult = NicknameValidationResult.Duplicate),
                    onIntent = {},
                )
            }
        }
        composeRule.onNodeWithText("이미 사용 중인 닉네임이에요.").assertIsDisplayed()
    }

    @Test
    fun initial() {
        composeRule.setContent { CreateProfileScreenshotScenario.Render() }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(CreateProfileScreenshotScenario.FILE_NAME)
    }
}
