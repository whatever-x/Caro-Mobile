package com.whatever.caro.feature.profile.components

import androidx.compose.ui.layout.ContentScale
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe

class ProfileCtaButtonTest : FunSpec() {
    init {
        test("CTA 로딩 로띠를 버튼 텍스트 영역에 맞게 확대한다") {
            ProfileButtonLoadingContentScale shouldBe ContentScale.Crop
        }

        test("CTA 로딩 로띠 크롭 창이 점 밴드(y 350~510)를 모두 포함한다") {
            val compositionSize = 800f
            val boxWidth = 57f
            val boxHeight = 17f

            val scale = boxWidth / compositionSize
            val visibleSourceHeight = boxHeight / scale
            val cropCenter = compositionSize / 2 + ProfileButtonLoadingAlignment.verticalBias * compositionSize / 2

            (cropCenter - visibleSourceHeight / 2) shouldBeLessThan 350f
            (cropCenter + visibleSourceHeight / 2) shouldBeGreaterThan 510f
        }

        test("CTA 로딩 중에는 클릭을 비활성화한다") {
            isProfileCtaButtonClickEnabled(enabled = true, isLoading = true).shouldBeFalse()
        }
    }
}
