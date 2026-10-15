package com.whatever.caro.feature.deck.create

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.captureRoboImage
import platform.Foundation.NSUserDefaults
import kotlin.test.Test

class CreateDeckScreenshotIosTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
    @Test
    fun initial() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey("AppleLanguages")
        defaults.setObject(listOf(CreateDeckScreenshotScenario.LOCALE), forKey = "AppleLanguages")

        try {
            runSkikoComposeUiTest(
                size = Size(CreateDeckScreenshotScenario.WIDTH_DP.toFloat(), CreateDeckScreenshotScenario.HEIGHT_DP.toFloat()),
            ) {
                setContent { CreateDeckScreenshotScenario.Render() }
                waitForIdle()
                onRoot().captureRoboImage(this, filePath = CreateDeckScreenshotScenario.FILE_NAME)
            }
        } finally {
            if (previousLanguages == null) {
                defaults.removeObjectForKey("AppleLanguages")
            } else {
                defaults.setObject(previousLanguages, forKey = "AppleLanguages")
            }
        }
    }
}
