package com.whatever.caro.screenshot

import androidx.compose.ui.unit.IntSize
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScreenshotContractTest : FunSpec() {
    init {
        test("both platforms expand every common scenario to one capture per language") {
            val scenarios = {
                listOf(
                    ScreenshotScenario("fixture.initial", render = {}),
                    ScreenshotScenario("setting.notification.small.large-font", render = {}),
                )
            }
            val android = screenshotCaptures("android", scenarios)
            val ios = screenshotCaptures("ios", scenarios)
            android.map { it.scenario.id to it.language }.toSet() shouldBe ios.map { it.scenario.id to it.language }.toSet()
            android.size shouldBe 2 * ScreenshotConfiguration.languages.size
            (android + ios).map { it.fileName }.distinct().size shouldBe android.size + ios.size
            android.single { it.language == "en" && it.scenario.id == "fixture.initial" }.fileName shouldBe
                "fixture.initial__en__pixel10-pro.png"
            (android + ios).filter { it.scenario.id == "setting.notification.small.large-font" }.forEach {
                it.profile.widthPx / it.profile.density shouldBe 320f
                it.profile.heightPx / it.profile.density shouldBe 480f
                it.profile.fontScale shouldBe 1.5f
            }
        }

        test("default geometry is exact pixels and density is independent from physical ppi") {
            val profile = ScreenshotConfiguration.profiles.single { it.platform == "android" && it.variant == "default" }
            profile.widthPx shouldBe 1280
            profile.heightPx shouldBe 2856
            profile.density shouldBe 3f
            val capture = screenshotCaptures("android") { listOf(ScreenshotScenario("fixture.initial", render = {})) }.first()
            assertScreenshotGeometry(capture, IntSize(1280, 2856))
            shouldThrow<IllegalStateException> { assertScreenshotGeometry(capture, IntSize(1281, 2856)) }
        }

        test("content-sized roots are measured and cannot exceed their virtual canvas") {
            val capture = screenshotCaptures("ios") { listOf(ScreenshotScenario("setting.notification.permission", render = {})) }.first()
            assertScreenshotGeometry(capture, IntSize(990, 600))
            shouldThrow<IllegalStateException> { assertScreenshotGeometry(capture, IntSize(1207, 600)) }
        }

        test("duplicate executable registrations fail before rendering") {
            shouldThrow<IllegalArgumentException> {
                screenshotCaptures(
                    "android",
                ) { listOf(ScreenshotScenario("same.id", render = {}), ScreenshotScenario("same.id", render = {})) }
            }
        }

        test("scoped settings restore on success and rendering failure") {
            var value: String? = null
            withRestoredSetting({ value }, { value = it }, "ko") { value shouldBe "ko" }
            value shouldBe null
            value = "original"
            shouldThrow<IllegalStateException> {
                withRestoredSetting({ value }, { value = it }, "en") {
                    value shouldBe "en"
                    error("render failed")
                }
            }
            value shouldBe "original"
        }
    }
}
