package com.whatever.caro.feature.importer

import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.support.FailingList
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.ForcedError
import com.whatever.caro.feature.importer.support.GatedDispatcher
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportTestTables.ANKI_GRAMMAR
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.MarkingDispatcher
import com.whatever.caro.feature.importer.support.keepOnlyGrammar
import com.whatever.caro.feature.importer.support.pickedFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 설정 화면의 처리 중 뒤로 가기와 실패: 그 처리만 멈추거나 실패 대화상자를 띄우고, 화면과 초안과 원본은 남는다. */
class ImportDeckViewModelSettingsStopTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val ankiSummary by lazy { ImportTestTables.apkgSummary(ImportTestTables.ANKI_MAIN) }
    private val ankiFull by lazy { ImportTestTables.apkg(ImportTestTables.ANKI_MAIN, "default") }
    private val grammarOnly by lazy { ImportTestTables.apkg(ImportTestTables.ANKI_MAIN, "selection-0") }
    private val subdeck = ImportStep.Settings(SettingsKind.Subdeck, SettingsEntry.FromPreview)

    init {
        rig.installOn(this)

        context("처리 중 뒤로 가기") {
            test("하위 덱 [적용] 중 뒤로 가면 읽기를 멈추고 그 화면에 머물며 고른 하위 덱이 남는다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ankiSummary).returns(ankiFull)
                    val gate = reader.suspends()
                    val harness = subdeckOpened(reader)
                    keepOnlyGrammar(harness)
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    gate.complete(grammarOnly)
                    advanceUntilIdle()

                    harness.state.step shouldBe subdeck
                    harness.state.loading shouldBe null
                    harness.state.dialog shouldBe null
                    harness.state.draftSelection.subdeckIds shouldBe setOf(ANKI_GRAMMAR)
                    harness.state.isSettingsApplyEnabled shouldBe true
                    harness.reader.cancelledReads shouldBe 1
                    harness.tempStorage.regularFiles().size shouldBe 1
                }
            }

            test("카드 구성 [적용]으로 미리보기를 만드는 중 뒤로 가면 만들기를 멈추고 바꾼 구성이 남는다") {
                runTest(rig.dispatcher) {
                    val default = GatedDispatcher(rig.dispatcher)
                    val harness = pickedFile(rig, "words.txt", FakeImportSourceReader().returns(ImportTestTables.text()), default = default)
                    val applied = harness.state.mappings
                    val typeId = applied.keys.single()
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.CardLayout))
                    harness.send(ImportDeckIntent.ToggleField(typeId, FieldMapping.FRONT, "열 2"))
                    advanceUntilIdle()
                    val draft = harness.state.draftMappings
                    draft shouldNotBe applied

                    default.hold()
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    default.release()
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Settings(SettingsKind.CardLayout, SettingsEntry.FromPreview)
                    harness.state.loading shouldBe null
                    harness.state.dialog shouldBe null
                    harness.state.draftMappings shouldBe draft
                }
            }

            test("시트를 다시 읽는 중 뒤로 가면 읽기를 멈추고 초안은 직전 시트로 남는다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(3, 2)))
                    val gate = reader.suspends()
                    val harness = pickedFile(rig, "book.xlsx", reader)
                    val applied = harness.state.table
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Sheet))
                    harness.send(ImportDeckIntent.SelectSheet(1))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    gate.complete(ImportTestTables.xlsx(sheetRows = listOf(3, 2), selectedSheet = 1))
                    advanceUntilIdle()

                    val state = harness.state
                    state.step shouldBe ImportStep.Settings(SettingsKind.Sheet, SettingsEntry.FromPreview)
                    state.loading shouldBe null
                    state.dialog shouldBe null
                    state.pendingDraftSelection shouldBe null
                    state.draftSelection.sheetIndex shouldBe 0
                    state.draftTable shouldBe applied
                }
            }

            test("구분 기준을 다시 나누는 중 뒤로 가면 나누기만 멈추고 화면에 머문다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                    reader.suspends()
                    val harness = pickedFile(rig, "words.txt", reader)
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter))
                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Settings(SettingsKind.Delimiter, SettingsEntry.FromPreview)
                    harness.state.isResplitting shouldBe false
                    harness.state.dialog shouldBe null
                    harness.state.draftSelection.delimiter shouldBe null
                    harness.reader.cancelledReads shouldBe 1
                }
            }
        }

        context("실패") {
            test("[적용] 뒤 읽기가 실패하면 실패 대화상자를 띄우고 [확인]을 누르면 그 화면에 머물며 원본이 남는다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader()
                            .returns(ankiSummary)
                            .returns(ankiFull)
                            .fails(ForcedError())
                            .returns(grammarOnly)
                    val harness = subdeckOpened(reader)
                    keepOnlyGrammar(harness)

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe ImportDialog.PreviewFailed
                    harness.state.loading shouldBe null
                    harness.state.step shouldBe subdeck
                    harness.tempStorage.regularFiles().size shouldBe 1

                    harness.send(ImportDeckIntent.DismissDialog)
                    advanceUntilIdle()
                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe subdeck
                    harness.state.draftSelection.subdeckIds shouldBe setOf(ANKI_GRAMMAR)

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()
                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.table shouldBe grammarOnly
                }
            }

            test("시트를 다시 읽다 실패하면 실패 대화상자를 띄우고 초안은 직전 시트로 남는다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(3, 2))).fails(ForcedError())
                    val harness = pickedFile(rig, "book.xlsx", reader)
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Sheet))
                    harness.send(ImportDeckIntent.SelectSheet(1))
                    advanceUntilIdle()

                    harness.state.dialog shouldBe ImportDialog.PreviewFailed
                    harness.state.loading shouldBe null
                    harness.state.pendingDraftSelection shouldBe null
                    harness.state.draftSelection.sheetIndex shouldBe 0
                    harness.tempStorage.regularFiles().size shouldBe 1
                }
            }

            test("다시 나눈 표로 첫 카드를 만들다 실패하면 실패 대화상자를 띄우고 초안은 직전 값으로 남는다") {
                runTest(rig.dispatcher) {
                    val broken = ImportTestTables.text(delimiter = ",").let { table -> table.copy(rows = FailingList(table.rows) { true }) }
                    val harness = pickedFile(rig, "words.txt", FakeImportSourceReader().returns(ImportTestTables.text()).returns(broken))
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter))
                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()

                    harness.state.dialog shouldBe ImportDialog.PreviewFailed
                    harness.state.isResplitting shouldBe false
                    harness.state.draftSelection.delimiter shouldBe null
                }
            }

            test("[적용]으로 미리보기를 만들다 Error가 나도 덮개를 걷고 실패 대화상자를 띄운 채 그 화면에 머문다") {
                runTest(rig.dispatcher) {
                    val default = MarkingDispatcher(rig.dispatcher)
                    val text = ImportTestTables.text()
                    val table = text.copy(rows = FailingList(text.rows) { default.isRunning })
                    val harness = pickedFile(rig, "words.txt", FakeImportSourceReader().returns(table), default = default)
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.CardLayout))
                    advanceUntilIdle()

                    default.arm()
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe ImportDialog.PreviewFailed
                    harness.state.loading shouldBe null
                    harness.state.step shouldBe ImportStep.Settings(SettingsKind.CardLayout, SettingsEntry.FromPreview)
                }
            }

            test("[적용] 뒤 미리보기로 넘길 덱 이름을 계산하다 실패해도 덮개를 걷고 실패 대화상자를 띄운 채 그 화면에 머문다") {
                runTest(rig.dispatcher) {
                    // 하위 덱 목록은 미리보기 계산이 읽지 않고 덱 이름을 정할 때 처음 읽는다.
                    val broken = grammarOnly.copy(subdecks = FailingList(grammarOnly.subdecks) { true })
                    val harness = subdeckOpened(FakeImportSourceReader().returns(ankiSummary).returns(ankiFull).returns(broken))
                    keepOnlyGrammar(harness)

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe ImportDialog.PreviewFailed
                    harness.state.loading shouldBe null
                    harness.state.step shouldBe subdeck
                    harness.state.draftSelection.subdeckIds shouldBe setOf(ANKI_GRAMMAR)
                }
            }
        }
    }

    private fun TestScope.subdeckOpened(reader: FakeImportSourceReader): ImportViewModelHarness {
        val harness = pickedFile(rig, "deck.apkg", reader)
        harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Subdeck))
        advanceUntilIdle()
        return harness
    }
}
