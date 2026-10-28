package com.whatever.caro.feature.login

import com.whatever.caro.screenshot.captureIosScreenshots
import kotlin.test.Test

class LoginScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(LoginScreenshotScenario::scenarios)
}
