package com.whatever.caro.feature.importer

import app.cash.turbine.test
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.GatedDispatcher
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.ScriptedCardRepository
import com.whatever.caro.feature.importer.support.saveScenario
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** 파일 선택 결과를 받는 시점의 가드: 파일 정보를 읽는 동안의 뒤로 가기와 원본 선택이 아닌 단계에 온 결과를 확인한다. */
class ImportDeckViewModelPickerTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        context("파일 정보를 읽는 동안의 뒤로 가기") {
            test("로딩 표시가 뜨기 전에 뒤로 가면 화면을 나가지 않고 읽기를 취소해 처음 단계에 머문다") {
                runTest(rig.dispatcher) {
                    val io = GatedDispatcher(rig.dispatcher)
                    val harness = rig.create(ioDispatcher = io)
                    val file = FakePickedImportFile.ofSize("words.txt", size = 100)
                    harness.send(ImportDeckIntent.ClickPickFile)
                    advanceUntilIdle()
                    io.hold()
                    harness.pick(file)
                    runCurrent()
                    harness.state.loading shouldBe null
                    harness.state.isPickerOpen shouldBe true

                    harness.viewModel.sideEffect.test {
                        awaitItem() shouldBe ImportDeckSideEffect.OpenFilePicker
                        harness.send(ImportDeckIntent.ClickBack)
                        runCurrent()
                        io.release()
                        advanceUntilIdle()
                        expectNoEvents()
                    }

                    harness.state shouldBe ImportDeckState()
                    harness.reader.calls.shouldBeEmpty()
                    file.discardCount shouldBe 1
                    harness.tempStorage.regularFiles().shouldBeEmpty()
                }
            }
        }

        context("원본 선택이 아닌 단계에 온 선택 결과") {
            test("저장 중에 온 파일은 읽지 않고 핸들만 정리한다") {
                runTest(rig.dispatcher) {
                    val cards = ScriptedCardRepository().apply { callDelayMillis = 100 }
                    val scenario = saveScenario(rig, cards, total = 2_500)
                    scenario.send(ImportDeckIntent.ClickCreateDeck)
                    runCurrent()
                    val before = scenario.state
                    val readsBefore = scenario.harness.reader.calls.size
                    val file = FakePickedImportFile.ofSize("late.txt", size = 100)

                    scenario.harness.pick(file)
                    runCurrent()

                    scenario.state shouldBe before
                    scenario.state.step shouldBe ImportStep.Saving
                    scenario.harness.reader.calls.size shouldBe readsBefore
                    file.discardCount shouldBe 1
                }
            }

            test("선택 단계에 온 파일은 읽지 않고 핸들만 정리하며 읽은 내용을 유지한다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()))
                    harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                    advanceUntilIdle()
                    val before = harness.state
                    val file = FakePickedImportFile.ofSize("again.txt", size = 100)

                    harness.pick(file)
                    advanceUntilIdle()

                    harness.state shouldBe before
                    harness.reader.calls.size shouldBe 1
                    file.discardCount shouldBe 1
                }
            }

            test("선택 단계에 온 취소, 크기 초과, 실패 결과는 무시한다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()))
                    harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                    advanceUntilIdle()
                    val before = harness.state

                    harness.pickResult(PickResult.Cancelled)
                    harness.pickResult(PickResult.TooLarge(displayName = "big.apkg", sizeBytes = 200L * 1024 * 1024))
                    harness.pickResult(PickResult.Failed(IllegalStateException("late")))
                    advanceUntilIdle()

                    harness.state shouldBe before
                }
            }
        }
    }
}
