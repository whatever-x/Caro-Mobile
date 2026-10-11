package com.whatever.caro.feature.importer.logic

import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.mvi.SavingStatus
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SavingProgressStyleTest : FunSpec() {
    private val beforeFirstResponse = SavingProgress(deckName = "JLPT N5 단어", totalCards = 1_512)
    private val afterFirstResponse = beforeFirstResponse.copy(savedCards = 1_000)

    init {
        test("첫 응답 전이고 동작 줄이기가 꺼져 있으면 진행률 없는 막대다") {
            savingProgressStyle(beforeFirstResponse, reduceMotion = false) shouldBe SavingProgressStyle.Indeterminate
        }

        test("첫 응답 전이고 동작 줄이기가 켜져 있으면 정지 막대다") {
            savingProgressStyle(beforeFirstResponse, reduceMotion = true) shouldBe SavingProgressStyle.Static
        }

        test("첫 응답 뒤에는 진행률 막대다") {
            savingProgressStyle(afterFirstResponse, reduceMotion = false) shouldBe SavingProgressStyle.Determinate
        }

        test("첫 응답 뒤에는 동작 줄이기가 켜져 있어도 진행률 막대다") {
            savingProgressStyle(afterFirstResponse, reduceMotion = true) shouldBe SavingProgressStyle.Determinate
        }

        test("첫 응답 전에는 다시 시도하는 중이어도 같은 규칙을 따른다") {
            val retrying = beforeFirstResponse.copy(status = SavingStatus.Retrying)

            savingProgressStyle(retrying, reduceMotion = false) shouldBe SavingProgressStyle.Indeterminate
        }

        test("첫 응답 전에는 멈추는 중이어도 같은 규칙을 따른다") {
            val stopping = beforeFirstResponse.copy(status = SavingStatus.Stopping)

            savingProgressStyle(stopping, reduceMotion = true) shouldBe SavingProgressStyle.Static
        }

        test("전부 저장했으면 진행률 막대다") {
            val finished = beforeFirstResponse.copy(savedCards = 1_512)

            savingProgressStyle(finished, reduceMotion = false) shouldBe SavingProgressStyle.Determinate
        }
    }
}
