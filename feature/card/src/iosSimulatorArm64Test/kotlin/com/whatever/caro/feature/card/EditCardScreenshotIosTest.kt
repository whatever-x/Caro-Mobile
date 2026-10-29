package com.whatever.caro.feature.card

import kotlin.test.Test

class EditCardScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(EditCardScreenshotScenario::scenarios)
}
