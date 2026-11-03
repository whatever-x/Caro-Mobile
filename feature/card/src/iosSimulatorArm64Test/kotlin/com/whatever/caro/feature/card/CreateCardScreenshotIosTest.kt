package com.whatever.caro.feature.card

import com.whatever.caro.core.screenshot.testing.captureIosScreenshots
import kotlin.test.Test

class CreateCardScreenshotIosTest {
    @Test
    fun `모든 시나리오의 한국어와 영어 스크린샷을 캡처한다`() = captureIosScreenshots(CreateCardScreenshotScenario::scenarios)
}
