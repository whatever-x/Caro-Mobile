package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.mvi.SettingsReason
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.GatedDispatcher
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportTestTables.ANKI_GRAMMAR
import com.whatever.caro.feature.importer.support.ImportTestTables.ANKI_N4
import com.whatever.caro.feature.importer.support.ImportTestTables.ANKI_N5
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.ReadCall
import com.whatever.caro.feature.importer.support.keepOnlyGrammar
import com.whatever.caro.feature.importer.support.pickedFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 먼저 고칠 것: 첫 읽기 판정이 하위 덱이나 시트부터 고르게 하는 경우와 그 화면의 [미리보기], 뒤로 가기를 확인한다. */
class ImportDeckViewModelGateTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val ankiSummary by lazy { ImportTestTables.apkgSummary(ImportTestTables.ANKI_MAIN) }
    private val grammarOnly by lazy { ImportTestTables.apkg(ImportTestTables.ANKI_MAIN, "selection-0") }
    private val subdeckGate = ImportStep.Settings(SettingsKind.Subdeck, SettingsEntry.Gate)
    private val sheetGate = ImportStep.Settings(SettingsKind.Sheet, SettingsEntry.Gate)

    // 하위 덱 셋(310, 403, 504장)의 합계만 넘고 각각은 상한 안에 든다.
    private val ankiLimits = ImportLimitsConfig(maxCards = 1_000)
    private val sheetLimits = ImportLimitsConfig(maxCards = 3)

    init {
        rig.installOn(this)

        context("하위 덱부터 고르기") {
            test("Anki 노트 합계가 상한을 넘고 혼자서 상한 안에 드는 하위 덱이 있으면 하위 덱 고르기를 먼저 연다") {
                runTest(rig.dispatcher) {
                    val harness = pickedAnki(FakeImportSourceReader().returns(ankiSummary))

                    val state = harness.state
                    state.step shouldBe subdeckGate
                    state.loading shouldBe null
                    state.table shouldBe null
                    state.draftTable shouldBe ankiSummary
                    state.draftSelection.subdeckIds shouldBe ankiSummary.subdecks.map { subdeck -> subdeck.id }.toSet()
                    state.settingsReason shouldBe SettingsReason.SubdeckOverLimit
                    state.isSettingsApplyEnabled shouldBe false
                    harness.reader.calls.size shouldBe 1
                }
            }

            test("혼자서 상한 안에 드는 하위 덱이 없으면 너무 많다는 대화상자로 막는다") {
                runTest(rig.dispatcher) {
                    val harness = pickedAnki(FakeImportSourceReader().returns(ankiSummary), ImportLimitsConfig(maxCards = 300))

                    harness.state.dialog shouldBe ImportDialog.TooManyCards(isPaste = false)
                    harness.state.step shouldBe ImportStep.Source
                }
            }

            test("하위 덱을 줄여 상한 안에 들면 사유가 사라지고 [미리보기]가 켜진다") {
                runTest(rig.dispatcher) {
                    val harness = pickedAnki(FakeImportSourceReader().returns(ankiSummary))

                    harness.send(ImportDeckIntent.ToggleSubdeck(ANKI_N5))
                    advanceUntilIdle()

                    harness.state.draftSelection.subdeckIds shouldBe setOf(ANKI_GRAMMAR, ANKI_N4)
                    harness.state.settingsReason shouldBe null
                    harness.state.isSettingsApplyEnabled shouldBe true
                }
            }

            test("전체 선택을 누르면 모두 풀려 사유 없이 버튼이 꺼지고 다시 누르면 모두 골라 상한 사유가 돌아온다") {
                runTest(rig.dispatcher) {
                    val harness = pickedAnki(FakeImportSourceReader().returns(ankiSummary))

                    harness.send(ImportDeckIntent.ToggleAllSubdecks)
                    advanceUntilIdle()

                    harness.state.draftSelection.subdeckIds shouldBe emptySet()
                    harness.state.settingsReason shouldBe null
                    harness.state.isSettingsApplyEnabled shouldBe false

                    harness.send(ImportDeckIntent.ToggleAllSubdecks)
                    advanceUntilIdle()

                    harness.state.draftSelection.subdeckIds shouldBe setOf(ANKI_GRAMMAR, ANKI_N4, ANKI_N5)
                    harness.state.settingsReason shouldBe SettingsReason.SubdeckOverLimit
                }
            }

            test("없는 하위 덱을 고르는 입력은 무시한다") {
                runTest(rig.dispatcher) {
                    val harness = pickedAnki(FakeImportSourceReader().returns(ankiSummary))
                    val before = harness.state

                    harness.send(ImportDeckIntent.ToggleSubdeck("unknown"))
                    advanceUntilIdle()

                    harness.state shouldBe before
                }
            }

            test("[미리보기] 결과가 0장이면 그 화면에 머물고 사유를 두며 하위 덱을 바꾸기 전까지 버튼을 끈다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ankiSummary).returns(emptyRows(ankiSummary))
                    val harness = pickedAnki(reader)
                    keepOnlyGrammar(harness)

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.state.step shouldBe subdeckGate
                    harness.state.dialog shouldBe null
                    harness.state.settingsReason shouldBe SettingsReason.NoCards
                    harness.state.isSettingsApplyEnabled shouldBe false

                    harness.send(ImportDeckIntent.ToggleSubdeck(ANKI_N4))
                    advanceUntilIdle()

                    harness.state.settingsReason shouldBe null
                    harness.state.isSettingsApplyEnabled shouldBe true
                }
            }

            test("[미리보기]를 연달아 눌러도 한 번만 읽는다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ankiSummary).returns(grammarOnly)
                    val harness = pickedAnki(reader)
                    keepOnlyGrammar(harness)

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.reader.calls.size shouldBe 2
                    harness.state.step shouldBe ImportStep.Preview
                }
            }
        }

        context("시트부터 고르기") {
            test("엑셀 첫 시트가 상한을 넘고 후보 시트가 있으면 시트 고르기를 먼저 연다") {
                runTest(rig.dispatcher) {
                    val first = ImportTestTables.xlsx(sheetRows = listOf(5, 2))
                    val harness = pickedXlsx(FakeImportSourceReader().returns(first))

                    val state = harness.state
                    state.step shouldBe sheetGate
                    state.draftTable shouldBe first
                    state.draftSelection.sheetIndex shouldBe 0
                    state.settingsReason shouldBe SettingsReason.SheetOverLimit
                    state.isSettingsApplyEnabled shouldBe false
                }
            }

            test("다시 읽은 후보 시트가 상한을 넘고 다른 후보가 있으면 그 시트로 시트 고르기를 먼저 연다") {
                runTest(rig.dispatcher) {
                    val second = ImportTestTables.xlsx(sheetRows = listOf(0, 4, 2), selectedSheet = 1, hasHeaderRow = false)
                    val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(0, 4, 2))).returns(second)
                    val harness = pickedXlsx(reader)

                    harness.state.step shouldBe sheetGate
                    harness.state.draftTable shouldBe second
                    harness.state.draftSelection.sheetIndex shouldBe 1
                    harness.state.settingsReason shouldBe SettingsReason.SheetOverLimit
                }
            }

            test("다른 시트를 고르면 덮개를 띄우고 그 시트를 다시 읽어 초안에 넣는다") {
                runTest(rig.dispatcher) {
                    val second = ImportTestTables.xlsx(sheetRows = listOf(5, 2), selectedSheet = 1)
                    val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(5, 2)))
                    val gate = reader.suspends()
                    val harness = pickedXlsx(reader)

                    harness.send(ImportDeckIntent.SelectSheet(1))
                    advanceUntilIdle()
                    harness.state.loading shouldBe ImportLoading.ReadingFile("book.xlsx")
                    harness.state.pendingDraftSelection
                        .shouldNotBeNull()
                        .sheetIndex shouldBe 1
                    val call =
                        harness.reader.calls
                            .last()
                            .shouldBeInstanceOf<ReadCall.FileRead>()
                    call.sheetIndex shouldBe 1
                    call.hasHeaderRow shouldBe null

                    gate.complete(second)
                    advanceUntilIdle()

                    val state = harness.state
                    state.loading shouldBe null
                    state.pendingDraftSelection shouldBe null
                    state.draftTable shouldBe second
                    state.draftSelection.sheetIndex shouldBe 1
                    state.settingsReason shouldBe null
                    state.isSettingsApplyEnabled shouldBe true
                }
            }

            test("[미리보기]를 누르면 덮개를 거쳐 고른 시트로 미리보기로 간다") {
                runTest(rig.dispatcher) {
                    val second = ImportTestTables.xlsx(sheetRows = listOf(5, 2), selectedSheet = 1)
                    val default = GatedDispatcher(rig.dispatcher)
                    val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(5, 2))).returns(second)
                    val harness = pickedXlsx(reader, default)
                    harness.send(ImportDeckIntent.SelectSheet(1))
                    advanceUntilIdle()

                    default.hold()
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()
                    harness.state.loading shouldBe ImportLoading.BuildingPreview("book.xlsx")
                    default.release()
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.table shouldBe second
                    harness.state.selection.sheetIndex shouldBe 1
                    harness.state.deckName shouldBe "book - 시트2"
                    harness.reader.calls.size shouldBe 2
                }
            }

            test("빈 시트를 고르면 다시 읽지 않고 빈 시트 사유를 두며 버튼을 끈다") {
                runTest(rig.dispatcher) {
                    val harness = pickedXlsx(FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(5, 2, 0))))

                    harness.send(ImportDeckIntent.SelectSheet(2))
                    advanceUntilIdle()

                    harness.state.draftSelection.sheetIndex shouldBe 2
                    harness.state.settingsReason shouldBe SettingsReason.SheetEmpty
                    harness.state.isSettingsApplyEnabled shouldBe false
                    harness.reader.calls.size shouldBe 1
                }
            }
        }

        context("뒤로 가기") {
            test("먼저 고칠 것 화면에서 뒤로 가면 처음으로 돌아갈지 묻고 [취소]를 누르면 그 화면에 머문다") {
                runTest(rig.dispatcher) {
                    val harness = pickedAnki(FakeImportSourceReader().returns(ankiSummary))

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    harness.state.dialog shouldBe ImportDialog.DiscardConfirm

                    harness.send(ImportDeckIntent.DismissDialog)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe subdeckGate
                    harness.state.draftTable shouldBe ankiSummary
                }
            }

            test("처음으로 돌아갈지 묻는 대화상자에서 [돌아가기]를 누르면 원본을 버리고 처음으로 간다") {
                runTest(rig.dispatcher) {
                    val harness = pickedAnki(FakeImportSourceReader().returns(ankiSummary))
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ConfirmDiscard)
                    advanceUntilIdle()

                    harness.state shouldBe ImportDeckState()
                    harness.tempStorage.regularFiles().shouldBeEmpty()
                }
            }

            test("[미리보기]로 고른 하위 덱을 읽는 중 뒤로 가면 읽기만 멈추고 묻지 않은 채 그 화면에 머문다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ankiSummary)
                    val gate = reader.suspends()
                    val harness = pickedAnki(reader)
                    keepOnlyGrammar(harness)
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    gate.complete(grammarOnly)
                    advanceUntilIdle()

                    harness.state.step shouldBe subdeckGate
                    harness.state.loading shouldBe null
                    harness.state.dialog shouldBe null
                    harness.state.draftSelection.subdeckIds shouldBe setOf(ANKI_GRAMMAR)
                    harness.tempStorage.regularFiles().size shouldBe 1
                }
            }
        }
    }

    private fun TestScope.pickedAnki(
        reader: FakeImportSourceReader,
        limits: ImportLimitsConfig = ankiLimits,
    ): ImportViewModelHarness = pickedFile(rig, "deck.apkg", reader, limits)

    private fun TestScope.pickedXlsx(
        reader: FakeImportSourceReader,
        default: CoroutineDispatcher = rig.dispatcher,
    ): ImportViewModelHarness = pickedFile(rig, "book.xlsx", reader, sheetLimits, default)

    /** 고른 하위 덱에 노트는 있지만 앞뒤가 모두 빈 행만 있는 결과다. */
    private fun emptyRows(summary: ParsedTable): ParsedTable {
        val type = summary.noteTypes.first()
        val row = ParsedRow(noteTypeId = type.id, subdeckId = ANKI_GRAMMAR, fields = type.fieldNames.associateWith { "" })
        return summary.copy(rows = listOf(row))
    }
}
