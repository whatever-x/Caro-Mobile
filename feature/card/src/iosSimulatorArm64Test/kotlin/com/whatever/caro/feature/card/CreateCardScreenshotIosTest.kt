package com.whatever.caro.feature.card

import com.whatever.caro.screenshot.captureIosScreenshots
import kotlin.test.Test

class CreateCardScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(CreateCardScreenshotScenario::scenarios)
}
