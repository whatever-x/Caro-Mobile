package com.whatever.caro.feature.home

import kotlin.test.Test

class HomeScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(HomeScreenshotScenario::scenarios)
}
