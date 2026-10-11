package com.whatever.caro.feature.importer

import app.cash.turbine.test
import com.whatever.caro.core.data.repository.deck.DeckRepository
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.exception.CaroServerException
import com.whatever.caro.core.model.exception.NetworkException
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.EMPTY_SIDE_TEXT
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.ScriptedCardRepository
import com.whatever.caro.feature.importer.support.openDeckInfo
import com.whatever.caro.feature.importer.support.openMappingFromFile
import com.whatever.caro.feature.importer.support.openPreview
import com.whatever.caro.feature.importer.support.runSaveToEnd
import com.whatever.caro.feature.importer.support.saveScenario
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 경계의 입력 검증: 연결 Intent의 값, 대화상자가 떠 있을 때의 뒤로 가기, 미리보기 계산이 던질 때를 확인한다. */
class ImportDeckViewModelBoundaryTest : FunSpec() {
    private val rig = ImportViewModelRig()

    // 순회하면 던지는 목록. 미리보기 계산이 행을 읽는 시점에 예외가 나게 한다.
    private class FailingRows : AbstractList<ParsedRow>() {
        override val size: Int = 1

        override fun get(index: Int): ParsedRow = throw IllegalStateException("rows are unreadable")
    }

    init {
        rig.installOn(this)

        context("ToggleField 값 검증") {
            test("면 이름이 앞면이나 뒷면이 아니면 연결을 바꾸지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = textMapping()
                    val before = harness.state.mappings

                    harness.send(ImportDeckIntent.ToggleField(TEXT, "middle", "열 1"))
                    advanceUntilIdle()

                    harness.state.mappings shouldBe before
                }
            }

            test("노트 유형에 없는 항목 이름이면 연결을 바꾸지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = textMapping()
                    val before = harness.state.mappings

                    harness.send(ImportDeckIntent.ToggleField(TEXT, FieldMapping.BACK, "없는 항목"))
                    advanceUntilIdle()

                    harness.state.mappings shouldBe before
                }
            }
        }

        context("대화상자가 떠 있을 때의 뒤로 가기") {
            test("닫을 수 있는 대화상자는 뒤로 가기가 대화상자만 닫고 단계는 옮기지 않는다") {
                runTest(rig.dispatcher) {
                    val repository = mock<DeckRepository> { everySuspend { createDeck(any(), any()) } throws networkTimeout() }
                    val harness = deckInfo(repository)
                    harness.send(ImportDeckIntent.ClickCreateDeck)
                    advanceUntilIdle()
                    harness.state.dialog shouldBe ImportDialog.DeckCreateFailed

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.dialog.shouldBeNull()
                    harness.state.step shouldBe ImportStep.DeckInfo
                }
            }

            test("처음 단계의 읽기 실패 대화상자는 뒤로 가기가 닫기만 하고 화면을 나가지 않는다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().fails(ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced"))
                    val harness = rig.create(reader = reader)
                    harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100))
                    advanceUntilIdle()
                    harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, "apkg", isPaste = false)

                    harness.viewModel.sideEffect.test {
                        harness.send(ImportDeckIntent.ClickBack)
                        advanceUntilIdle()
                        expectNoEvents()
                    }

                    harness.state.dialog.shouldBeNull()
                    harness.state.step shouldBe ImportStep.Source
                }
            }

            test("닫을 수 없는 저장 대화상자는 뒤로 가기를 무시한다") {
                runTest(rig.dispatcher) {
                    val cards = ScriptedCardRepository(CaroServerException(code = "A002", message = "A002", debugMessage = "A002"))
                    val scenario = saveScenario(rig, cards)
                    runSaveToEnd(scenario)
                    val dialog = scenario.state.dialog
                    (dialog is ImportDialog.PartialFailure) shouldBe true

                    scenario.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    scenario.state.dialog shouldBe dialog
                    scenario.state.step shouldBe ImportStep.Saving
                }
            }
        }

        context("미리보기 계산 실패") {
            test("계산이 던지면 오버레이를 걷고 연결 단계에 머물러 다시 누를 수 있다") {
                runTest(rig.dispatcher) {
                    val table = ImportTestTables.text().copy(rows = FailingRows())
                    val harness = rig.create(reader = FakeImportSourceReader().returns(table))
                    openMappingFromFile(harness, "words.txt")
                    val mappingsBefore = harness.state.mappings

                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    advanceUntilIdle()

                    harness.state.loading.shouldBeNull()
                    harness.state.step shouldBe ImportStep.Mapping
                    harness.state.preview.shouldBeNull()
                    harness.state.mappings shouldBe mappingsBefore

                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    advanceUntilIdle()

                    harness.state.loading.shouldBeNull()
                    harness.state.step shouldBe ImportStep.Mapping
                }
            }
        }
    }

    private fun TestScope.textMapping(table: ParsedTable = ImportTestTables.text()): ImportViewModelHarness {
        val harness = rig.create(reader = FakeImportSourceReader().returns(table))
        openMappingFromFile(harness, "words.txt")
        return harness
    }

    private fun TestScope.deckInfo(repository: DeckRepository): ImportViewModelHarness {
        val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()), deckRepository = repository)
        openMappingFromFile(harness, "words.txt")
        openPreview(harness)
        openDeckInfo(harness)
        harness.state.step shouldBe ImportStep.DeckInfo
        return harness
    }

    private companion object {
        const val TEXT = "text"

        fun networkTimeout() = NetworkException.Timeout(debugMessage = "timeout")
    }
}
