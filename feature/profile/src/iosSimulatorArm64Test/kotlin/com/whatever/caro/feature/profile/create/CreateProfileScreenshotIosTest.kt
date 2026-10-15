package com.whatever.caro.feature.profile.create

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.captureRoboImage
import platform.Foundation.NSUserDefaults
import kotlin.test.Test

class CreateProfileScreenshotIosTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
    @Test
    fun initial() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey("AppleLanguages")
        defaults.setObject(listOf(CreateProfileScreenshotScenario.LOCALE), forKey = "AppleLanguages")

        try {
            runSkikoComposeUiTest(
                size = Size(CreateProfileScreenshotScenario.WIDTH_DP.toFloat(), CreateProfileScreenshotScenario.HEIGHT_DP.toFloat()),
            ) {
                setContent { CreateProfileScreenshotScenario.Render() }
                waitForIdle()
                onRoot().captureRoboImage(this, filePath = CreateProfileScreenshotScenario.FILE_NAME)
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
