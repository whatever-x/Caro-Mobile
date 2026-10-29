package com.whatever.caro.feature.login

import kotlin.test.Test

class LoginScreenshotIosTest {
    @Test
    fun captures() = captureIosScreenshots(LoginScreenshotScenario::scenarios)
}
