package com.whatever.caro.feature.deck.create

import com.whatever.caro.screenshot.captureIosScreenshots
import kotlin.test.Test

class CreateDeckScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(CreateDeckScreenshotScenario::scenarios)
}
