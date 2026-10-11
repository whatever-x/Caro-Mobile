package com.whatever.caro.core.designsystem.foundation

import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CaroPageSizeTest :
    FunSpec({
        test("페이지 좌우 여백은 28dp다") {
            CaroPageHorizontalPadding shouldBe 28.dp
        }

        test("하단 CTA 버튼 최소 높이는 56dp다") {
            CaroCtaButtonHeight shouldBe 56.dp
        }

        test("팁 줄 앞 점 크기는 4dp다") {
            CaroTipDotSize shouldBe 4.dp
        }
    })
