package com.whatever.caro.feature.card

import com.whatever.caro.screenshot.captureIosScreenshots
import kotlin.test.Test

class EditCardScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(EditCardScreenshotScenario::scenarios)
}
