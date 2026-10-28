package com.whatever.caro.feature.learning

import com.whatever.caro.screenshot.captureIosScreenshots
import kotlin.test.Test

class LearningScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(LearningScreenshotScenario::scenarios)
}
