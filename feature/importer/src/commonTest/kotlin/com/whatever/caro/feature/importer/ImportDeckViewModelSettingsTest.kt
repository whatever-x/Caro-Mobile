package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.GatedDispatcher
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportTestTables.ANKI_GRAMMAR
import com.whatever.caro.feature.importer.support.ImportTestTables.ANKI_N4
import com.whatever.caro.feature.importer.support.ImportTestTables.ANKI_N5
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.ReadCall
import com.whatever.caro.feature.importer.support.pickedFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 미리보기의 [변경]으로 연 설정 화면: 초안을 채우고, [적용]하면 덮개를 거쳐 미리보기로, 처리 중이 아닐 때 뒤로 가면 바꾼 내용 없이 미리보기로 간다. */
class ImportDeckViewModelSettingsTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val ankiSummary by lazy { ImportTestTables.apkgSummary(ImportTestTables.ANKI_MAIN) }
    private val ankiFull by lazy { ImportTestTables.apkg(ImportTestTables.ANKI_MAIN, "default") }
    private val grammarOnly by lazy { ImportTestTables.apkg(ImportTestTables.ANKI_MAIN, "selection-0") }

    init {
        rig.installOn(this)

        context("[변경]으로 열기") {
            test("[변경]을 누르면 해당 설정 화면이 열리고 초안이 적용된 설정으로 채워진다") {
                runTest(rig.dispatcher) {
                    val harness = pickedFile(rig, "words.txt", FakeImportSourceReader().returns(ImportTestTables.text()))
                    val applied = harness.state

                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.CardLayout))
                    advanceUntilIdle()

                    val state = harness.state
                    state.step shouldBe ImportStep.Settings(SettingsKind.CardLayout, SettingsEntry.FromPreview)
                    state.draftTable shouldBe applied.table
                    state.draftSelection shouldBe applied.selection
                    state.draftMappings shouldBe applied.mappings
                    state.draftFirstCards.values.map { card -> card.front } shouldBe listOf("열 1-1")
                    state.settingsReason shouldBe null
                    state.isSettingsApplyEnabled shouldBe true
                }
            }

            test("형식에 없는 설정의 [변경]은 무시한다") {
                runTest(rig.dispatcher) {
                    val harness = pickedFile(rig, "words.txt", FakeImportSourceReader().returns(ImportTestTables.text()))

                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Subdeck))
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Sheet))
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.draftTable shouldBe null
                }
            }

            test("띠의 [구분 기준 변경]을 누르면 구분 기준 확인이 열린다") {
                runTest(rig.dispatcher) {
                    val table = ImportTestTables.text(rows = 2).let { it.copy(layout = it.layout?.copy(singleFieldRowCount = 2)) }
                    val harness = pickedFile(rig, "words.txt", FakeImportSourceReader().returns(table))
                    harness.state.delimiterBannerRowCount shouldBe 2

                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter))
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Settings(SettingsKind.Delimiter, SettingsEntry.FromPreview)
                    harness.state.draftFirstCards.values
                        .single()
                        .front shouldBe "열 1-1"
                }
            }

            test("설정 화면에서 처리 중이 아닐 때 뒤로 가면 바꾼 내용 없이 미리보기로 간다") {
                runTest(rig.dispatcher) {
                    val harness =
                        pickedFile(rig, "book.xlsx", FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(3, 2))))
                    val applied = harness.state
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.CardLayout))
                    harness.send(ImportDeckIntent.ToggleNoteType(harness.singleTypeId()))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    val state = harness.state
                    state.step shouldBe ImportStep.Preview
                    state.mappings shouldBe applied.mappings
                    state.preview shouldBe applied.preview
                    state.draftTable shouldBe null
                    state.draftMappings.shouldBeEmpty()
                }
            }

            test("하위 덱을 바꾼 채 [적용] 없이 뒤로 가면 미리보기의 선택과 표가 그대로다") {
                runTest(rig.dispatcher) {
                    val harness = pickedFile(rig, "deck.apkg", FakeImportSourceReader().returns(ankiSummary).returns(ankiFull))
                    val applied = harness.state
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Subdeck))
                    harness.send(ImportDeckIntent.ToggleSubdeck(ANKI_N4))
                    advanceUntilIdle()
                    harness.state.draftSelection.subdeckIds shouldNotBe applied.selection.subdeckIds

                    leaveWithoutApplying(harness, applied)
                }
            }

            test("시트를 바꿔 다시 읽은 뒤 [적용] 없이 뒤로 가면 미리보기의 선택과 표가 그대로다") {
                runTest(rig.dispatcher) {
                    val second = ImportTestTables.xlsx(sheetRows = listOf(3, 2), selectedSheet = 1)
                    val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(3, 2))).returns(second)
                    val harness = pickedFile(rig, "book.xlsx", reader)
                    val applied = harness.state
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Sheet))
                    harness.send(ImportDeckIntent.SelectSheet(1))
                    advanceUntilIdle()
                    harness.state.draftSelection.sheetIndex shouldBe 1
                    harness.state.draftTable shouldBe second

                    leaveWithoutApplying(harness, applied)
                }
            }

            test("구분 기준을 바꿔 다시 나눈 뒤 [적용] 없이 뒤로 가면 미리보기의 선택과 표가 그대로다") {
                runTest(rig.dispatcher) {
                    val resplit = ImportTestTables.text(rows = 2, delimiter = ",")
                    val harness = pickedFile(rig, "words.txt", FakeImportSourceReader().returns(ImportTestTables.text()).returns(resplit))
                    val applied = harness.state
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter))
                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()
                    harness.state.draftSelection.delimiter shouldBe ','
                    harness.state.draftTable shouldBe resplit

                    leaveWithoutApplying(harness, applied)
                }
            }
        }

        context("[적용]") {
            test("하위 덱 [적용]은 덮개를 거쳐 고른 덱의 행을 읽어 미리보기를 만든다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ankiSummary).returns(ankiFull)
                    val gate = reader.suspends()
                    val harness = pickedFile(rig, "deck.apkg", reader)
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Subdeck))
                    harness.send(ImportDeckIntent.ToggleSubdeck(ANKI_N4))
                    harness.send(ImportDeckIntent.ToggleSubdeck(ANKI_N5))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()
                    harness.state.loading shouldBe ImportLoading.BuildingPreview("deck.apkg")
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                        .subdeckIds shouldBe setOf(ANKI_GRAMMAR)

                    gate.complete(grammarOnly)
                    advanceUntilIdle()

                    val state = harness.state
                    state.step shouldBe ImportStep.Preview
                    state.table shouldBe grammarOnly
                    state.selection.subdeckIds shouldBe setOf(ANKI_GRAMMAR)
                    state.preview
                        .shouldNotBeNull()
                        .cards.size shouldBe state.filteredCards.size
                    state.draftTable shouldBe null
                }
            }

            test("하위 덱을 둘 이상 고른 채 [적용]하면 고른 하위 덱을 모두 읽는다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ankiSummary).returns(ankiFull).returns(ankiFull)
                    val harness = pickedFile(rig, "deck.apkg", reader)
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Subdeck))
                    harness.send(ImportDeckIntent.ToggleSubdeck(ANKI_N5))
                    advanceUntilIdle()
                    harness.state.draftSelection.subdeckIds shouldBe setOf(ANKI_GRAMMAR, ANKI_N4)

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                        .subdeckIds shouldBe setOf(ANKI_GRAMMAR, ANKI_N4)
                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.selection.subdeckIds shouldBe setOf(ANKI_GRAMMAR, ANKI_N4)
                }
            }

            test("하위 덱을 바꿔 적용해도 항목 이름이 같은 유형의 카드 구성은 남는다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ankiSummary).returns(ankiFull).returns(grammarOnly)
                    val harness = pickedFile(rig, "deck.apkg", reader)
                    val cloze = ankiFull.noteTypes.first { type -> type.isCloze }
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.CardLayout))
                    harness.send(ImportDeckIntent.ToggleNoteType(cloze.id))
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Subdeck))
                    harness.send(ImportDeckIntent.ToggleSubdeck(ANKI_N4))
                    harness.send(ImportDeckIntent.ToggleSubdeck(ANKI_N5))
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.mappings[cloze.id]
                        .shouldNotBeNull()
                        .excluded shouldBe true
                }
            }

            test("시트 [적용]은 다시 읽은 시트로 미리보기를 만들고 덱 이름을 새 시트로 다시 채운다") {
                runTest(rig.dispatcher) {
                    val second = ImportTestTables.xlsx(sheetRows = listOf(3, 2), selectedSheet = 1)
                    val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(3, 2))).returns(second)
                    val harness = pickedFile(rig, "book.xlsx", reader)
                    harness.state.deckName shouldBe "book - 시트1"
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Sheet))
                    harness.send(ImportDeckIntent.SelectSheet(1))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    val state = harness.state
                    state.step shouldBe ImportStep.Preview
                    state.table shouldBe second
                    state.selection.sheetIndex shouldBe 1
                    state.deckName shouldBe "book - 시트2"
                }
            }

            test("구분 기준을 바꾸면 결과 표 자리에서 다시 나누고 그동안 [적용]이 꺼진다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                    val gate = reader.suspends()
                    val harness = pickedFile(rig, "words.txt", reader)
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()

                    harness.state.isResplitting shouldBe true
                    harness.state.loading shouldBe null
                    harness.state.isSettingsApplyEnabled shouldBe false
                    harness.state.pendingDraftSelection
                        .shouldNotBeNull()
                        .delimiter shouldBe ','
                    harness.reader.calls
                        .last()
                        .delimiter shouldBe ','

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()
                    harness.state.loading shouldBe null

                    val resplit = ImportTestTables.text(rows = 2, delimiter = ",", columns = 3)
                    gate.complete(resplit)
                    advanceUntilIdle()

                    val state = harness.state
                    state.isResplitting shouldBe false
                    state.draftTable shouldBe resplit
                    state.draftSelection.delimiter shouldBe ','
                    state.draftMappings.values
                        .single()
                        .frontFields shouldBe listOf("열 1")
                    state.draftFirstCards.values
                        .single()
                        .back shouldBe "열 2-1\n열 3-1"
                    state.isSettingsApplyEnabled shouldBe true
                }
            }

            test("구분 기준 [적용]은 다시 나눈 초안으로 미리보기로 간다") {
                runTest(rig.dispatcher) {
                    val resplit = ImportTestTables.text(rows = 2, delimiter = ",")
                    val harness = pickedFile(rig, "words.txt", FakeImportSourceReader().returns(ImportTestTables.text()).returns(resplit))
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter))
                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.table shouldBe resplit
                    harness.state.selection.delimiter shouldBe ','
                    harness.state.preview
                        .shouldNotBeNull()
                        .cards.size shouldBe 2
                }
            }

            test("설정을 적용해 다시 만든 미리보기도 빈 면에 처음 읽을 때 받은 문구를 쓴다") {
                runTest(rig.dispatcher) {
                    val resplit = ImportTestTables.text(columns = 1, delimiter = ",")
                    val reader = FakeImportSourceReader().returns(ImportTestTables.text(columns = 1)).returns(resplit)
                    val harness = pickedFile(rig, "words.txt", reader)
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter))
                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.state.table shouldBe resplit
                    harness.state.preview
                        .shouldNotBeNull()
                        .cards
                        .first()
                        .back shouldBe ImportViewModelHarness.EMPTY_SIDE_TEXT
                }
            }

            test("카드 구성 [적용]은 덮개를 거쳐 바꾼 구성으로 미리보기를 만든다") {
                runTest(rig.dispatcher) {
                    val default = GatedDispatcher(rig.dispatcher)
                    val harness = pickedFile(rig, "words.txt", FakeImportSourceReader().returns(ImportTestTables.text()), default = default)
                    val typeId = harness.singleTypeId()
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.CardLayout))
                    harness.send(ImportDeckIntent.ToggleField(typeId, FieldMapping.BACK, "열 1"))
                    harness.send(ImportDeckIntent.ToggleField(typeId, FieldMapping.FRONT, "열 2"))
                    advanceUntilIdle()
                    harness.state.draftFirstCards[typeId]
                        .shouldNotBeNull()
                        .front shouldBe "열 2-1"

                    default.hold()
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()
                    harness.state.loading shouldBe ImportLoading.BuildingPreview("words.txt")
                    default.release()
                    advanceUntilIdle()

                    val state = harness.state
                    state.step shouldBe ImportStep.Preview
                    state.mappings[typeId]
                        .shouldNotBeNull()
                        .frontFields shouldBe listOf("열 2")
                    state.preview
                        .shouldNotBeNull()
                        .cards
                        .first()
                        .front shouldBe "열 2-1"
                }
            }

            test("붙여넣기의 [적용] 덮개에는 파일 이름이 없다") {
                runTest(rig.dispatcher) {
                    val default = GatedDispatcher(rig.dispatcher)
                    val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()), default = default)
                    harness.pasteNext("a\tb")
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.CardLayout))
                    advanceUntilIdle()

                    default.hold()
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.state.loading shouldBe ImportLoading.BuildingPreview(fileName = null)
                    default.release()
                    advanceUntilIdle()
                    harness.state.step shouldBe ImportStep.Preview
                }
            }
        }

        context("Anki 텍스트") {
            test("머리말이 열 이름을 주면 첫 줄 체크를 바꿔도 다시 읽지 않는다") {
                runTest(rig.dispatcher) {
                    val table =
                        ImportTestTables.text().copy(
                            layout = ParsedLayout(delimiter = "\t", hasHeaderRow = false, columnsFromHeader = true),
                        )
                    val harness = pickedFile(rig, "anki.txt", FakeImportSourceReader().returns(table))
                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ChangeHeaderRow(true))
                    advanceUntilIdle()

                    harness.reader.calls.size shouldBe 1
                    harness.state.draftSelection.hasHeaderRow shouldBe null
                    harness.state.pendingDraftSelection shouldBe null
                }
            }
        }
    }

    private fun TestScope.leaveWithoutApplying(
        harness: ImportViewModelHarness,
        applied: ImportDeckState,
    ) {
        harness.send(ImportDeckIntent.ClickBack)
        advanceUntilIdle()

        val state = harness.state
        state.step shouldBe ImportStep.Preview
        state.selection shouldBe applied.selection
        state.table shouldBe applied.table
        state.draftTable shouldBe null
    }
}
