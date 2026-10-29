package com.whatever.caro.feature.card.detail

import com.whatever.caro.feature.card.captureIosScreenshots
import kotlin.test.Test

class CardDetailScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(CardDetailScreenshotScenario::scenarios)
}
