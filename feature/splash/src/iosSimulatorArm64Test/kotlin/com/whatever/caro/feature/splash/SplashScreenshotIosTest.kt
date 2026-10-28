package com.whatever.caro.feature.splash

import com.whatever.caro.screenshot.captureIosScreenshots
import kotlin.test.Test

class SplashScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(SplashScreenshotScenario::scenarios)
}
