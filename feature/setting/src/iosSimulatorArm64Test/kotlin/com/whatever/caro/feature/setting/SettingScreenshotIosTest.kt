package com.whatever.caro.feature.setting

import com.whatever.caro.screenshot.captureIosScreenshots
import kotlin.test.Test

class SettingScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(SettingScreenshotScenario::scenarios)
}
