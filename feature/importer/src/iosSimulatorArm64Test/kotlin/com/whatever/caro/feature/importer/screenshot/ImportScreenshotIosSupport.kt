package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.captureRoboImage
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
internal fun captureImportScreenshot(
    id: String,
    content: @Composable () -> Unit,
) {
    val defaults = NSUserDefaults.standardUserDefaults
    val previousLanguages = defaults.objectForKey("AppleLanguages")
    defaults.setObject(listOf(ImportScreenshotScenario.LOCALE), forKey = "AppleLanguages")

    try {
        runSkikoComposeUiTest(
            size = Size(ImportScreenshotScenario.WIDTH_DP.toFloat(), ImportScreenshotScenario.HEIGHT_DP.toFloat()),
        ) {
            setContent(content)
            waitForIdle()
            onRoot().captureRoboImage(this, filePath = ImportScreenshotScenario.fileName(id))
        }
    } finally {
        if (previousLanguages == null) {
            defaults.removeObjectForKey("AppleLanguages")
        } else {
            defaults.setObject(previousLanguages, forKey = "AppleLanguages")
        }
    }
}
