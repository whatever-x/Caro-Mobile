package com.whatever.caro.feature.learning

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-rKR-w393dp-h852dp-xxhdpi")
class LearningScreenshotAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initial() {
        composeRule.setContent { LearningScreenshotScenario.Render() }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(LearningScreenshotScenario.FILE_NAME)
    }
}
