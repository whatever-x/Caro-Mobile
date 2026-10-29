package com.whatever.caro.feature.splash

import kotlin.test.Test

class SplashScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(SplashScreenshotScenario::scenarios)
}
