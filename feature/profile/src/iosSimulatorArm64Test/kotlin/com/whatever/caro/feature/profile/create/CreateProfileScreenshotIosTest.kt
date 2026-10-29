package com.whatever.caro.feature.profile.create

import com.whatever.caro.feature.profile.captureIosScreenshots
import kotlin.test.Test

class CreateProfileScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(CreateProfileScreenshotScenario::scenarios)
}
