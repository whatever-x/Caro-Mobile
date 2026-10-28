package com.whatever.caro.feature.deck.detail

import com.whatever.caro.screenshot.captureIosScreenshots
import kotlin.test.Test

class DeckDetailScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(DeckDetailScreenshotScenario::scenarios)
}
