package com.whatever.caro.feature.deck.detail

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.whatever.caro.core.screenshot.testing.ScreenshotCapture
import com.whatever.caro.core.screenshot.testing.captureAndroidScreenshot
import com.whatever.caro.core.screenshot.testing.screenshotAndroidRule
import com.whatever.caro.core.screenshot.testing.screenshotCaptures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
internal class DeckDetailScreenshotAndroidTest(
    private val capture: ScreenshotCapture,
) {
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val captureRule = screenshotAndroidRule(capture, composeRule)

    @Test
    fun `지정한 언어와 프로필로 시나리오 스크린샷을 캡처한다`() = captureAndroidScreenshot(composeRule, capture)

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun captures(): List<Array<Any>> = screenshotCaptures("android", DeckDetailScreenshotScenario::scenarios).map { arrayOf(it) }
    }
}
