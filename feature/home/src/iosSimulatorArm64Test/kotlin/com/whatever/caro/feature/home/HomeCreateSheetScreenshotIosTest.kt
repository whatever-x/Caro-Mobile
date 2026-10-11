package com.whatever.caro.feature.home

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.captureRoboImage
import platform.Foundation.NSUserDefaults
import kotlin.test.Test

class HomeCreateSheetScreenshotIosTest {
    @OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
    @Test
    fun createSheet() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.objectForKey("AppleLanguages")
        defaults.setObject(listOf(HomeCreateSheetScreenshotScenario.LOCALE), forKey = "AppleLanguages")

        try {
            runSkikoComposeUiTest(
                size = Size(HomeCreateSheetScreenshotScenario.WIDTH_DP.toFloat(), HomeCreateSheetScreenshotScenario.HEIGHT_DP.toFloat()),
            ) {
                setContent { HomeCreateSheetScreenshotScenario.Render() }
                waitForIdle()
                // 시트는 별도 창이라 root가 둘이 되어 onRoot()가 실패한다. 첫 root가 배경과 스크림, 시트 내용을 모두 담는다.
                onAllNodes(isRoot())[0].captureRoboImage(this, filePath = HomeCreateSheetScreenshotScenario.FILE_NAME)
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
