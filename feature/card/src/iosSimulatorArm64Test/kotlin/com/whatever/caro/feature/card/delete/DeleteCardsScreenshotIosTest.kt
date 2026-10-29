package com.whatever.caro.feature.card.delete

import com.whatever.caro.feature.card.captureIosScreenshots
import kotlin.test.Test

class DeleteCardsScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(DeleteCardsScreenshotScenario::scenarios)
}
