package com.whatever.caro.feature.card

import kotlin.test.Test

class CreateCardScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(CreateCardScreenshotScenario::scenarios)
}
