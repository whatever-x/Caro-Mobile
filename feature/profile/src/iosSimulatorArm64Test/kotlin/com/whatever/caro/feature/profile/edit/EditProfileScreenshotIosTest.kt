package com.whatever.caro.feature.profile.edit

import com.whatever.caro.feature.profile.captureIosScreenshots
import kotlin.test.Test

class EditProfileScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(EditProfileScreenshotScenario::scenarios)
}
