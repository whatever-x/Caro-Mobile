package com.whatever.caro.feature.home

import kotlin.test.Test

class LibraryScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(LibraryScreenshotScenario::scenarios)
}
