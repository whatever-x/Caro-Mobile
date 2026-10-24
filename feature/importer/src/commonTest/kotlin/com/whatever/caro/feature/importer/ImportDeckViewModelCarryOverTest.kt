package com.whatever.caro.feature.importer

import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ImportMappingTables
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.openMappingFromFile
import com.whatever.caro.feature.importer.support.openMappingFromPaste
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 항목 연결에서 뒤로 가기와, 선택 단계를 오간 뒤 고친 연결을 유지하거나 기본값으로 돌리는 규칙을 확인한다. */
class ImportDeckViewModelCarryOverTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val summary by lazy { ImportTestTables.apkgSummary("modern_anki21b_main.apkg") }
    private val allSubdecks by lazy { ImportTestTables.apkg("modern_anki21b_main.apkg", "default") }
    private val grammarOnly by lazy { ImportTestTables.apkg("modern_anki21b_main.apkg", "selection-0") }

    init {
        rig.installOn(this)

        context("Mapping에서 뒤로 가기") {
            test("apkg는 확인 없이 SubdeckSelect로 가고 고른 하위 덱을 유지한다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(summary).returns(allSubdecks)
                    val harness = rig.create(reader = reader)
                    harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ToggleSubdeck(GRAMMAR))
                    harness.send(ImportDeckIntent.ClickSelectionNext)
                    advanceUntilIdle()
                    harness.state.step shouldBe ImportStep.Mapping
                    val selectionBefore = harness.state.selection

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.SubdeckSelect
                    harness.state.dialog shouldBe null
                    harness.state.selection shouldBe selectionBefore
                    harness.state.selection.subdeckIds
                        .contains(GRAMMAR) shouldBe false
                }
            }

            test("xlsx는 SheetSelect로 가고 고른 시트를 유지한다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader().returns(ImportTestTables.xlsx()).returns(ImportTestTables.xlsx(selectedSheet = 1))
                    val harness = rig.create(reader = reader)
                    harness.pick(FakePickedImportFile.ofSize("book.xlsx", size = 100))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.SelectSheet(1))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ClickSelectionNext)
                    advanceUntilIdle()
                    harness.state.step shouldBe ImportStep.Mapping

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.SheetSelect
                    harness.state.dialog shouldBe null
                    harness.state.selection.sheetIndex shouldBe 1
                }
            }

            test("텍스트 파일은 TextCheck로 가고 고른 구분자를 유지한다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader().returns(ImportTestTables.text()).returns(ImportTestTables.text(delimiter = ","))
                    val harness = rig.create(reader = reader)
                    harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ClickSelectionNext)
                    advanceUntilIdle()
                    harness.state.step shouldBe ImportStep.Mapping

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.TextCheck
                    harness.state.dialog shouldBe null
                    harness.state.selection.delimiter shouldBe ','
                }
            }

            test("붙여넣기는 TextCheck로 가고 붙여 넣은 글을 유지한다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text(sourceName = PASTE_NAME)))
                    openMappingFromPaste(harness, PASTE_TEXT)

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.TextCheck
                    harness.state.isPaste shouldBe true
                    harness.state.dialog shouldBe null
                    harness.state.pasteText shouldBe PASTE_TEXT
                }
            }
        }

        context("연결 유지") {
            test("선택 단계로 돌아갔다가 노트 유형 집합이 같은 채로 다시 오면 고친 연결을 유지한다") {
                runTest(rig.dispatcher) {
                    val harness = apkgMapping(FakeImportSourceReader().returns(summary).returns(allSubdecks).returns(allSubdecks))
                    val defaults = harness.state.mappings
                    editMapping(harness)
                    val edited = harness.state.mappings
                    edited shouldNotBe defaults

                    backToSelectionAndNext(harness)

                    harness.state.step shouldBe ImportStep.Mapping
                    harness.state.mappings shouldBe edited
                }
            }

            test("다른 하위 덱을 골라 노트 유형 집합이 바뀌면 연결을 기본값으로 돌린다") {
                runTest(rig.dispatcher) {
                    val harness = apkgMapping(FakeImportSourceReader().returns(summary).returns(allSubdecks).returns(grammarOnly))
                    val defaults = harness.state.mappings
                    editMapping(harness)
                    harness.state.mappings shouldNotBe defaults
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    harness.state.step shouldBe ImportStep.SubdeckSelect
                    harness.send(ImportDeckIntent.ToggleSubdeck(VOCAB_N4))
                    harness.send(ImportDeckIntent.ToggleSubdeck(VOCAB_N5))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickSelectionNext)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Mapping
                    val mappings = harness.state.mappings
                    mappings.getValue(BASIC).backFields shouldBe listOf("Back")
                    (mappings[VOCAB]?.excluded ?: false) shouldBe false
                }
            }

            test("텍스트 확인에서 Mapping으로 갔다가 돌아와 다시 가도 고친 연결을 유지한다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()))
                    openMappingFromFile(harness, "words.txt")
                    val defaults = harness.state.mappings
                    harness.send(ImportDeckIntent.ToggleField(TEXT, FieldMapping.BACK, "열 2"))
                    advanceUntilIdle()
                    val edited = harness.state.mappings
                    edited shouldNotBe defaults

                    backToSelectionAndNext(harness)

                    harness.state.step shouldBe ImportStep.Mapping
                    harness.state.mappings shouldBe edited
                }
            }

            test("구분자를 바꿔 같은 유형의 열 이름이 달라지면 연결을 기본값으로 돌린다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ImportTestTables.text()).returns(ImportMappingTables.threeColumnText())
                    val harness = rig.create(reader = reader)
                    openMappingFromFile(harness, "words.txt")
                    harness.send(ImportDeckIntent.ToggleField(TEXT, FieldMapping.BACK, "열 2"))
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    harness.state.step shouldBe ImportStep.TextCheck
                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickSelectionNext)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Mapping
                    val mapping = harness.state.mappings.getValue(TEXT)
                    mapping.frontFields shouldBe listOf("열 1")
                    mapping.backFields shouldBe listOf("열 2", "열 3")
                }
            }

            test("새 파일을 읽으면 앞 파일에서 고친 연결을 이어받지 않는다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader()
                            .returns(
                                ImportTestTables.text(),
                            ).returns(ImportTestTables.text(sourceName = "other.txt"))
                    val harness = rig.create(reader = reader)
                    openMappingFromFile(harness, "words.txt")
                    harness.send(ImportDeckIntent.ToggleField(TEXT, FieldMapping.BACK, "열 2"))
                    advanceUntilIdle()
                    harness.state.mappings
                        .getValue(TEXT)
                        .backFields shouldBe emptyList()
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ConfirmDiscard)
                    advanceUntilIdle()
                    harness.state.step shouldBe ImportStep.Source

                    openMappingFromFile(harness, "other.txt")

                    harness.state.mappings
                        .getValue(TEXT)
                        .backFields shouldBe listOf("열 2")
                }
            }
        }
    }

    private fun TestScope.apkgMapping(reader: FakeImportSourceReader): ImportViewModelHarness {
        val harness = rig.create(reader = reader)
        openMappingFromFile(harness, "deck.apkg")
        return harness
    }

    /** Basic의 뒷면 항목과 Vocab의 가져오기를 고쳐 기본값과 다른 연결을 만든다. */
    private fun TestScope.editMapping(harness: ImportViewModelHarness) {
        harness.send(ImportDeckIntent.ToggleNoteType(VOCAB))
        harness.send(ImportDeckIntent.ToggleField(BASIC, FieldMapping.BACK, "Back"))
        advanceUntilIdle()
    }

    private fun TestScope.backToSelectionAndNext(harness: ImportViewModelHarness) {
        harness.send(ImportDeckIntent.ClickBack)
        advanceUntilIdle()
        harness.send(ImportDeckIntent.ClickSelectionNext)
        advanceUntilIdle()
    }

    private companion object {
        const val GRAMMAR = "1791602016834"
        const val VOCAB_N4 = "1791602016837"
        const val VOCAB_N5 = "1791602016836"
        const val BASIC = "1700000001"
        const val VOCAB = "1700000003"
        const val TEXT = "text"
        const val PASTE_NAME = "붙여 넣은 카드"
        const val PASTE_TEXT = "apple\t사과"
    }
}
