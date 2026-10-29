package com.whatever.caro.feature.learning

import kotlin.test.Test

class LearningScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(LearningScreenshotScenario::scenarios)
}
