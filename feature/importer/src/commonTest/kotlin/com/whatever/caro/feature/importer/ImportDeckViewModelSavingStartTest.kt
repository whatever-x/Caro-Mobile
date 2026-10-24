package com.whatever.caro.feature.importer

import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.GatedDispatcher
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.ScriptedCardRepository
import com.whatever.caro.feature.importer.support.saveScenario
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** 덱을 만든 뒤 저장을 시작하는 시점: 묶음 계획을 main 밖에서 하고, 저장 중에는 이전 단계가 쓰던 메모리를 놓는다. */
class ImportDeckViewModelSavingStartTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        test("묶음 계획은 default dispatcher가 일하기 전에는 끝나지 않아 Saving으로 가지 않는다") {
            runTest(rig.dispatcher) {
                val gate = GatedDispatcher(rig.dispatcher)
                val scenario = saveScenario(rig, ScriptedCardRepository(), defaultDispatcher = gate)
                gate.hold()

                scenario.send(ImportDeckIntent.ClickCreateDeck)
                runCurrent()

                scenario.state.step shouldBe ImportStep.CreatingDeck
                scenario.state.saving.shouldBeNull()
                scenario.cards.calls.shouldBeEmpty()

                gate.release()
                advanceUntilIdle()

                scenario.state.step shouldBe ImportStep.Saving
                scenario.cards.calls.size shouldBe 1
                scenario.effects shouldBe listOf(scenario.navigation(saved = 3, completed = true))
            }
        }

        test("저장을 시작하면 읽은 표와 미리보기, 연결, 원본 사본을 놓고 저장은 그대로 끝낸다") {
            runTest(rig.dispatcher) {
                val cards = ScriptedCardRepository().apply { callDelayMillis = 100 }
                val scenario = saveScenario(rig, cards, total = 2_500)
                scenario.harness.tempStorage
                    .regularFiles()
                    .isEmpty() shouldBe false

                scenario.send(ImportDeckIntent.ClickCreateDeck)
                runCurrent()

                scenario.state.step shouldBe ImportStep.Saving
                scenario.state.table.shouldBeNull()
                scenario.state.preview.shouldBeNull()
                scenario.state.mappings.isEmpty() shouldBe true
                scenario.state.saving?.totalCards shouldBe 2_500
                scenario.harness.tempStorage
                    .regularFiles()
                    .shouldBeEmpty()

                advanceUntilIdle()

                scenario.cards.calls.map { it.cards.size } shouldBe listOf(1_000, 1_000, 500)
                scenario.effects shouldBe listOf(scenario.navigation(saved = 2_500, completed = true))
            }
        }
    }
}
