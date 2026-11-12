package com.whatever.caro.feature.login

import com.whatever.caro.core.screenshot.testing.captureIosScreenshots
import kotlin.test.Test

class LoginScreenshotIosTest {
    @Test
    fun `모든 시나리오의 한국어와 영어 스크린샷을 캡처한다`() = captureIosScreenshots(LoginScreenshotScenario::scenarios)
}
