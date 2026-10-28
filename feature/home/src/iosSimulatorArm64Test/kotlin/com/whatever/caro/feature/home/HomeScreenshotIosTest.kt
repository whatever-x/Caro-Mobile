package com.whatever.caro.feature.home

import com.whatever.caro.screenshot.captureIosScreenshots
import kotlin.test.Test

class HomeScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(HomeScreenshotScenario::scenarios)
}
