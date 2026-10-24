package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.captureRoboImage
import platform.Foundation.NSThread
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
internal fun captureImportScreenshot(
    id: String,
    content: @Composable () -> Unit,
) {
    runImportScreenshotTest {
        setContent(content)
        waitForIdle()
        captureRoot(id)
    }
}

// 무한 반복 Lottie가 있어 자동 시계를 켜 두면 waitForIdle이 끝나지 않는다. 시계를 직접 움직이는 순서가 바뀌면 캡처가 달라진다.
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
internal fun captureImportLoadingScreenshot(
    id: String,
    content: @Composable () -> Unit,
) {
    runImportScreenshotTest {
        mainClock.autoAdvance = false
        setContent(content)
        mainClock.advanceTimeBy(ImportLoadingScreenshotScenario.OVERLAY_DELAY_PASSED_MILLIS)
        NSThread.sleepForTimeInterval(ImportLoadingScreenshotScenario.LOTTIE_LOAD_WAIT_MILLIS / MILLIS_PER_SECOND)
        waitForIdle()
        mainClock.advanceTimeBy(ImportLoadingScreenshotScenario.CAPTURE_CLOCK_MILLIS)
        captureRoot(id)
    }
}

private const val MILLIS_PER_SECOND = 1_000.0

@OptIn(ExperimentalTestApi::class)
private fun runImportScreenshotTest(block: suspend SkikoComposeUiTest.() -> Unit) {
    val defaults = NSUserDefaults.standardUserDefaults
    val previousLanguages = defaults.objectForKey("AppleLanguages")
    defaults.setObject(listOf(ImportScreenshotScenario.LOCALE), forKey = "AppleLanguages")

    try {
        runSkikoComposeUiTest(
            size = Size(ImportScreenshotScenario.WIDTH_DP.toFloat(), ImportScreenshotScenario.HEIGHT_DP.toFloat()),
            block = block,
        )
    } finally {
        if (previousLanguages == null) {
            defaults.removeObjectForKey("AppleLanguages")
        } else {
            defaults.setObject(previousLanguages, forKey = "AppleLanguages")
        }
    }
}

// 대화상자와 시트는 별도 창이라 root가 둘이 되어 onRoot()가 실패한다. 첫 root가 배경과 스크림, 창 내용을 모두 담는다.
@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
private fun SkikoComposeUiTest.captureRoot(id: String) {
    onAllNodes(isRoot())[0].captureRoboImage(this, filePath = ImportScreenshotScenario.fileName(id))
}
