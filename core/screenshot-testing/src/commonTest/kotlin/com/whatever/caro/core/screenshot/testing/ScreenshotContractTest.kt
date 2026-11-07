package com.whatever.caro.core.screenshot.testing

import androidx.compose.ui.unit.IntSize
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ScreenshotContractTest : FunSpec() {
    init {
        test("두 플랫폼은 모든 공통 시나리오를 언어별로 한 번씩 캡처한다") {
            val scenarios = {
                listOf(
                    ScreenshotScenario("fixture.initial", render = {}, expectedStrings = emptyList()),
                    ScreenshotScenario(
                        "fixture.small",
                        render = {},
                        expectedStrings = emptyList(),
                        variant = ScreenshotVariant.SMALL_LARGE_FONT,
                    ),
                )
            }
            val android = screenshotCaptures("android", scenarios)
            val ios = screenshotCaptures("ios", scenarios)
            android.map { it.scenario.id to it.language }.toSet() shouldBe ios.map { it.scenario.id to it.language }.toSet()
            android.size shouldBe 2 * ScreenshotConfiguration.languages.size
            (android + ios).map { it.fileName }.distinct().size shouldBe android.size + ios.size
            android.single { it.language == "en" && it.scenario.id == "fixture.initial" }.fileName shouldBe
                "fixture.initial__en__pixel10-pro.png"
            (android + ios).filter { it.scenario.id == "fixture.small" }.forEach {
                it.profile.widthPx / it.profile.density shouldBe 320f
                it.profile.heightPx / it.profile.density shouldBe 480f
                it.profile.fontScale shouldBe 1.5f
            }
        }

        test("기본 프로필은 정확한 픽셀 크기와 논리 밀도를 유지한다") {
            val profile = ScreenshotConfiguration.profiles.single { it.platform == "android" && it.variant == ScreenshotVariant.DEFAULT }
            profile.widthPx shouldBe 1280
            profile.heightPx shouldBe 2856
            profile.density shouldBe 3f
            val capture =
                screenshotCaptures("android") {
                    listOf(ScreenshotScenario("fixture.initial", render = {}, expectedStrings = emptyList()))
                }.first()
            assertScreenshotGeometry(capture, IntSize(1280, 2856))
            shouldThrow<IllegalStateException> { assertScreenshotGeometry(capture, IntSize(1281, 2856)) }
        }

        test("콘텐츠 크기의 다이얼로그는 가상 캔버스 범위를 넘을 수 없다") {
            val capture =
                screenshotCaptures("ios") {
                    listOf(
                        ScreenshotScenario(
                            "fixture.dialog",
                            render = {},
                            expectedStrings = emptyList(),
                            target = ScreenshotTarget.DIALOG,
                        ),
                    )
                }.first()
            assertScreenshotGeometry(capture, IntSize(990, 600))
            shouldThrow<IllegalStateException> { assertScreenshotGeometry(capture, IntSize(0, 600)) }
            shouldThrow<IllegalStateException> { assertScreenshotGeometry(capture, IntSize(1207, 600)) }
            shouldThrow<IllegalStateException> {
                assertScreenshotGeometry(capture, IntSize(capture.profile.widthPx, capture.profile.heightPx))
            }
        }

        test("좁은 화면 변형은 시나리오 ID와 관계없이 직접 선언한다") {
            for (platform in listOf("android", "ios")) {
                val captures =
                    screenshotCaptures(platform) {
                        listOf(
                            ScreenshotScenario(
                                "fixture.new-narrow-screen",
                                render = {},
                                expectedStrings = emptyList(),
                                variant = ScreenshotVariant.NARROW,
                            ),
                        )
                    }
                captures.forEach {
                    it.profile.widthPx / it.profile.density shouldBe 320f
                    it.profile.heightPx / it.profile.density shouldBe 700f
                    it.profile.fontScale shouldBe 1f
                }
            }
        }

        test("언어별 실행은 새로운 시나리오 인스턴스를 생성한다") {
            var factoryCalls = 0
            val captures =
                screenshotCaptures("android") {
                    factoryCalls++
                    listOf(ScreenshotScenario("fixture.initial", render = {}, expectedStrings = emptyList()))
                }
            factoryCalls shouldBe 2
            captures.map { it.language } shouldBe listOf("ko", "en")
            (captures[0].scenario === captures[1].scenario) shouldBe false
        }

        test("시나리오 ID는 안전한 파일명만 허용하고 예약 접미사는 끝에서만 제한한다") {
            for (id in listOf("../outside", "fixture__en", "fixture_actual", "fixture_compare", "fixture_before", "")) {
                shouldThrow<IllegalArgumentException> {
                    screenshotCaptures("android") {
                        listOf(ScreenshotScenario(id, render = {}, expectedStrings = emptyList()))
                    }
                }
            }
            val allowedIds = listOf("fixture_actual.detail", "fixture_compare_detail", "fixture_before-detail")
            val captures =
                screenshotCaptures("android") {
                    allowedIds.map { ScreenshotScenario(it, render = {}, expectedStrings = emptyList()) }
                }
            captures.map { it.scenario.id }.distinct() shouldBe allowedIds
        }

        test("중복 시나리오 ID는 렌더링 전에 거부한다") {
            shouldThrow<IllegalArgumentException> {
                screenshotCaptures(
                    "android",
                ) {
                    listOf(
                        ScreenshotScenario("same.id", render = {}, expectedStrings = emptyList()),
                        ScreenshotScenario("same.id", render = {}, expectedStrings = emptyList()),
                    )
                }
            }
        }
    }
}
