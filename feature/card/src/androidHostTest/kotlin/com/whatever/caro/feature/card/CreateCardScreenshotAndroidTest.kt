package com.whatever.caro.feature.card

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.whatever.caro.screenshot.ScreenshotCapture
import com.whatever.caro.screenshot.captureAndroidScreenshot
import com.whatever.caro.screenshot.screenshotCaptures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w427dp-h952dp-xxhdpi")
internal class CreateCardScreenshotAndroidTest(
    private val capture: ScreenshotCapture,
) {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun capture() = captureAndroidScreenshot(composeRule, capture)

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun captures(): List<Array<Any>> = screenshotCaptures("android", CreateCardScreenshotScenario::scenarios).map { arrayOf(it) }
    }
}
