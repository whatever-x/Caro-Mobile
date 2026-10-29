package com.whatever.caro.feature.setting

import kotlin.test.Test

class SettingScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(SettingScreenshotScenario::scenarios)
}
