package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage

internal const val IMPORT_SCREENSHOT_QUALIFIERS = "ko-rKR-w393dp-h852dp-xxhdpi"

internal fun ComposeContentTestRule.captureImportScreenshot(
    id: String,
    content: @Composable () -> Unit,
) {
    setContent(content)
    waitForIdle()
    onRoot().captureRoboImage(ImportScreenshotScenario.fileName(id))
}
