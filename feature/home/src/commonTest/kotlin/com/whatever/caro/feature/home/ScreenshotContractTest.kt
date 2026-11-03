package com.whatever.caro.feature.home

import com.whatever.caro.core.screenshot.testing.ScreenshotConfiguration
import com.whatever.caro.core.screenshot.testing.screenshotCaptures
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScreenshotContractTest : FunSpec() {
    init {
        test("화면별 팩토리는 두 플랫폼에서 자신의 시나리오 ID를 유지한다") {
            for (platform in listOf("android", "ios")) {
                val captures = screenshotCaptures(platform, HomeScreenshotScenario::scenarios)
                captures.map { it.scenario.id }.toSet() shouldBe setOf(HomeScreenshotScenario.ID)
                captures.map { it.language }.toSet() shouldBe ScreenshotConfiguration.languages.toSet()
            }
        }
    }
}
