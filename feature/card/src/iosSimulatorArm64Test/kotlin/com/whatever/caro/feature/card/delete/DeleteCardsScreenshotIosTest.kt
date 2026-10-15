package com.whatever.caro.feature.card.delete

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.captureRoboImage
import platform.Foundation.NSUserDefaults
import kotlin.test.Test

class DeleteCardsScreenshotIosTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
    @Test
    fun initial() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey("AppleLanguages")
        defaults.setObject(listOf(DeleteCardsScreenshotScenario.LOCALE), forKey = "AppleLanguages")

        try {
            runSkikoComposeUiTest(
                size = Size(DeleteCardsScreenshotScenario.WIDTH_DP.toFloat(), DeleteCardsScreenshotScenario.HEIGHT_DP.toFloat()),
            ) {
                setContent { DeleteCardsScreenshotScenario.Render() }
                waitForIdle()
                onRoot().captureRoboImage(this, filePath = DeleteCardsScreenshotScenario.FILE_NAME)
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
