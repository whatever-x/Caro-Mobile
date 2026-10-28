package com.whatever.caro.feature.profile.create

import com.whatever.caro.screenshot.captureIosScreenshots
import kotlin.test.Test

class CreateProfileScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(CreateProfileScreenshotScenario::scenarios)
}
