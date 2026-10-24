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

// 무한 반복 Lottie가 있어 자동 시계를 켜 두면 waitForIdle이 끝나지 않는다. 시계를 직접 움직이는 순서가 바뀌면 캡처가 달라진다.
internal fun ComposeContentTestRule.captureImportLoadingScreenshot(
    id: String,
    content: @Composable () -> Unit,
) {
    mainClock.autoAdvance = false
    setContent(content)
    mainClock.advanceTimeBy(ImportLoadingScreenshotScenario.OVERLAY_DELAY_PASSED_MILLIS)
    Thread.sleep(ImportLoadingScreenshotScenario.LOTTIE_LOAD_WAIT_MILLIS)
    waitForIdle()
    mainClock.advanceTimeBy(ImportLoadingScreenshotScenario.CAPTURE_CLOCK_MILLIS)
    onRoot().captureRoboImage(ImportScreenshotScenario.fileName(id))
}
