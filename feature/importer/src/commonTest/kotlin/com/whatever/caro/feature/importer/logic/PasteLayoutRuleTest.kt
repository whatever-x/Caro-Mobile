package com.whatever.caro.feature.importer.logic

import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

// 남는 높이는 제목과 안내를 보일 때를 기준으로 잰 값이다. 숨긴 뒤의 높이로 판정하면 숨김과 보임이 번갈아 바뀐다.
class PasteLayoutRuleTest : FunSpec() {
    init {
        test("키보드가 열렸고 입력칸에 남는 높이가 120dp 미만이면 제목과 안내를 숨긴다") {
            shouldHidePasteGuide(isImeVisible = true, inputHeightWithGuide = 119.dp) shouldBe true
        }

        test("키보드가 열려 있어도 남는 높이가 정확히 120dp이면 숨기지 않는다") {
            shouldHidePasteGuide(isImeVisible = true, inputHeightWithGuide = 120.dp) shouldBe false
        }

        test("키보드가 열려 있고 남는 높이가 120dp보다 크면 숨기지 않는다") {
            shouldHidePasteGuide(isImeVisible = true, inputHeightWithGuide = 336.dp) shouldBe false
        }

        test("남는 높이가 음수여도 키보드가 열려 있으면 숨긴다") {
            shouldHidePasteGuide(isImeVisible = true, inputHeightWithGuide = (-20).dp) shouldBe true
        }

        test("키보드가 닫혀 있으면 남는 높이가 모자라도 숨기지 않는다") {
            shouldHidePasteGuide(isImeVisible = false, inputHeightWithGuide = 80.dp) shouldBe false
        }

        test("키보드가 닫히면 숨겼던 제목과 안내를 다시 보여준다") {
            val whileOpen = shouldHidePasteGuide(isImeVisible = true, inputHeightWithGuide = 100.dp)
            val afterClose = shouldHidePasteGuide(isImeVisible = false, inputHeightWithGuide = 100.dp)

            listOf(whileOpen, afterClose) shouldBe listOf(true, false)
        }
    }
}
