package com.whatever.caro.feature.card.delete

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.whatever.caro.feature.card.ScreenshotCapture
import com.whatever.caro.feature.card.captureAndroidScreenshot
import com.whatever.caro.feature.card.screenshotAndroidRule
import com.whatever.caro.feature.card.screenshotCaptures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
internal class DeleteCardsScreenshotAndroidTest(
    private val capture: ScreenshotCapture,
) {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val captureRule = screenshotAndroidRule(capture, composeRule)

    @Test
    fun capture() = captureAndroidScreenshot(composeRule, capture)

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun captures(): List<Array<Any>> = screenshotCaptures("android", DeleteCardsScreenshotScenario::scenarios).map { arrayOf(it) }
    }
}
