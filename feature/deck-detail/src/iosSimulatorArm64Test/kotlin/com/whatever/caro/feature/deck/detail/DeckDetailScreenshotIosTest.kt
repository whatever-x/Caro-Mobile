package com.whatever.caro.feature.deck.detail

import kotlin.test.Test

class DeckDetailScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(DeckDetailScreenshotScenario::scenarios)
}
