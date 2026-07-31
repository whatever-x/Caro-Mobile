package com.whatever.caro.feature.profile.components

import androidx.compose.ui.layout.ContentScale
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.shouldBe

class ProfileCtaButtonTest : FunSpec() {
    init {
        test("CTA 로딩 로띠를 버튼 텍스트 영역에 맞게 확대한다") {
            ProfileButtonLoadingContentScale shouldBe ContentScale.Crop
        }

        test("CTA 로딩 중에는 클릭을 비활성화한다") {
            isProfileCtaButtonClickEnabled(enabled = true, isLoading = true).shouldBeFalse()
        }
    }
}
