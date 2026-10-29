package com.whatever.caro.feature.deck.edit

import com.whatever.caro.feature.deck.captureIosScreenshots
import kotlin.test.Test

class EditDeckScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(EditDeckScreenshotScenario::scenarios)
}
