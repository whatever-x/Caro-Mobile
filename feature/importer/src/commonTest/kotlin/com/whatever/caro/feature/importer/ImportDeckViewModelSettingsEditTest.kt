package com.whatever.caro.feature.importer

import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.logic.importableNoteTypes
import com.whatever.caro.feature.importer.logic.usesFieldPicker
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.mvi.SettingsReason
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.ReadCall
import com.whatever.caro.feature.importer.support.pickedFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 설정 화면 안에서 초안을 고치는 입력: 카드 구성, 구분 기준과 첫 줄, 시트의 첫 줄, 그리고 판정 사유. */
class ImportDeckViewModelSettingsEditTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val ankiFull by lazy { ImportTestTables.apkg(ImportTestTables.ANKI_MAIN, "default") }
    private val cardLayout = ImportStep.Settings(SettingsKind.CardLayout, SettingsEntry.FromPreview)

    init {
        rig.installOn(this)

        context("카드 구성") {
            test("모든 유형의 가져오기를 끄면 사유가 보이고 [적용]이 꺼지며 하나를 켜면 사라진다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader()
                            .returns(
                                ImportTestTables.apkgSummary(ImportTestTables.ANKI_MAIN),
                            ).returns(ankiFull)
                    val harness = opened("deck.apkg", reader, SettingsKind.CardLayout)
                    val types = importableNoteTypes(ankiFull)

                    types.forEach { type -> harness.send(ImportDeckIntent.ToggleNoteType(type.id)) }
                    advanceUntilIdle()

                    harness.state.settingsReason shouldBe SettingsReason.NoTypeEnabled
                    harness.state.isSettingsApplyEnabled shouldBe false
                    harness.state.draftFirstCards.shouldBeEmpty()

                    harness.send(ImportDeckIntent.ToggleNoteType(types.first().id))
                    advanceUntilIdle()

                    harness.state.settingsReason shouldBe null
                    harness.state.isSettingsApplyEnabled shouldBe true
                    harness.state.draftFirstCards.keys shouldBe setOf(types.first().id)
                }
            }

            test("[적용] 결과가 0장이면 그 화면에 머물고 사유를 보이며 설정을 바꾸기 전까지 버튼이 꺼진다") {
                runTest(rig.dispatcher) {
                    val harness = opened("words.txt", FakeImportSourceReader().returns(ImportTestTables.text()), SettingsKind.CardLayout)
                    val typeId = harness.singleTypeId()
                    harness.send(ImportDeckIntent.ToggleField(typeId, FieldMapping.FRONT, "열 1"))
                    harness.send(ImportDeckIntent.ToggleField(typeId, FieldMapping.BACK, "열 2"))
                    advanceUntilIdle()
                    harness.state.draftFirstCards.shouldBeEmpty()

                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.state.step shouldBe cardLayout
                    harness.state.loading shouldBe null
                    harness.state.settingsReason shouldBe SettingsReason.NoCards
                    harness.state.isSettingsApplyEnabled shouldBe false

                    harness.send(ImportDeckIntent.ToggleField(typeId, FieldMapping.FRONT, "열 1"))
                    advanceUntilIdle()

                    harness.state.settingsReason shouldBe null
                    harness.state.isSettingsApplyEnabled shouldBe true
                }
            }

            test("카드 구성을 바꾸면 바뀐 유형의 첫 카드만 다시 만들고 다른 유형의 첫 카드는 그대로 둔다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader()
                            .returns(
                                ImportTestTables.apkgSummary(ImportTestTables.ANKI_MAIN),
                            ).returns(ankiFull)
                    val harness = opened("deck.apkg", reader, SettingsKind.CardLayout)
                    val (kept, changed) = importableNoteTypes(ankiFull).take(2)
                    val keptCard = harness.state.draftFirstCards[kept.id].shouldNotBeNull()

                    harness.send(ImportDeckIntent.ToggleField(changed.id, FieldMapping.FRONT, changed.fieldNames.last()))
                    advanceUntilIdle()

                    harness.state.draftMappings
                        .getValue(changed.id)
                        .frontFields
                        .last() shouldBe changed.fieldNames.last()
                    harness.state.draftFirstCards[kept.id] shouldBeSameInstanceAs keptCard
                }
            }

            test("항목이 많은 유형도 항목 고르기에서 고른 항목이 초안에 고른 순서대로 들어간다") {
                runTest(rig.dispatcher) {
                    val table = ImportTestTables.text(columns = 13)
                    val harness = opened("words.txt", FakeImportSourceReader().returns(table), SettingsKind.CardLayout)
                    val typeId = harness.singleTypeId()
                    usesFieldPicker(table.noteTypes.single()) shouldBe true

                    harness.send(ImportDeckIntent.ToggleField(typeId, FieldMapping.FRONT, "열 13"))
                    advanceUntilIdle()

                    val mapping = harness.state.draftMappings[typeId].shouldNotBeNull()
                    mapping.frontFields shouldBe listOf("열 1", "열 13")
                    mapping.backFields shouldBe (2..12).map { number -> "열 $number" }
                }
            }

            test("없는 유형, 없는 면, 없는 항목을 고르는 입력은 무시한다") {
                runTest(rig.dispatcher) {
                    val harness = opened("words.txt", FakeImportSourceReader().returns(ImportTestTables.text()), SettingsKind.CardLayout)
                    val typeId = harness.singleTypeId()
                    val before = harness.state

                    harness.send(ImportDeckIntent.ToggleNoteType("unknown"))
                    harness.send(ImportDeckIntent.ToggleField("unknown", FieldMapping.FRONT, "열 1"))
                    harness.send(ImportDeckIntent.ToggleField(typeId, "side", "열 1"))
                    harness.send(ImportDeckIntent.ToggleField(typeId, FieldMapping.FRONT, "열 9"))
                    advanceUntilIdle()

                    harness.state shouldBe before
                }
            }
        }

        context("구분 기준과 첫 줄") {
            test("사유가 있는 채 다시 나누면 그동안 사유를 숨기고, 나누기를 멈추면 바뀌지 않은 초안의 사유와 꺼진 버튼이 돌아온다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader()
                            .returns(ImportTestTables.text(rows = 3))
                            .returns(ImportTestTables.text(rows = 4, delimiter = ","))
                    reader.suspends()
                    val harness = opened("words.txt", reader, SettingsKind.Delimiter, ImportLimitsConfig(maxCards = 3))
                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()
                    harness.state.settingsReason shouldBe SettingsReason.TooManyCards

                    harness.send(ImportDeckIntent.SelectDelimiter(';'))
                    advanceUntilIdle()
                    harness.state.isResplitting shouldBe true
                    harness.state.visibleSettingsReason shouldBe null

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    harness.state.draftSelection.delimiter shouldBe ','
                    harness.state.visibleSettingsReason shouldBe SettingsReason.TooManyCards
                    harness.state.isSettingsApplyEnabled shouldBe false
                }
            }

            test("지금과 같은 구분 기준이나 첫 줄 값을 다시 고르면 다시 읽지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = opened("words.txt", FakeImportSourceReader().returns(ImportTestTables.text()), SettingsKind.Delimiter)

                    harness.send(ImportDeckIntent.SelectDelimiter('\t'))
                    harness.send(ImportDeckIntent.ChangeHeaderRow(false))
                    advanceUntilIdle()

                    harness.reader.calls.size shouldBe 1
                }
            }

            test("다시 나누는 중에 구분 기준을 또 바꾸면 앞선 나누기를 버리고 마지막 값으로 나눈다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                    val first = reader.suspends()
                    val second = reader.suspends()
                    val harness = opened("words.txt", reader, SettingsKind.Delimiter)

                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.SelectDelimiter(';'))
                    advanceUntilIdle()
                    harness.reader.cancelledReads shouldBe 1
                    harness.state.pendingDraftSelection
                        .shouldNotBeNull()
                        .delimiter shouldBe ';'

                    val semicolon = ImportTestTables.text(rows = 2, delimiter = ";")
                    second.complete(semicolon)
                    first.complete(ImportTestTables.text(rows = 5, delimiter = ","))
                    advanceUntilIdle()

                    harness.state.draftTable shouldBe semicolon
                    harness.state.draftSelection.delimiter shouldBe ';'
                    harness.state.isResplitting shouldBe false
                }
            }

            test("쉼표로 다시 나누는 중에 첫 줄을 체크하면 쉼표를 유지한 채 다시 나눈다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                    reader.suspends()
                    val withHeader = reader.suspends()
                    val harness = opened("words.txt", reader, SettingsKind.Delimiter)

                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ChangeHeaderRow(true))
                    advanceUntilIdle()

                    val call = harness.reader.calls.last()
                    call.delimiter shouldBe ','
                    call.hasHeaderRow shouldBe true
                    val commaWithHeader = ImportTestTables.text(rows = 2, delimiter = ",", hasHeaderRow = true)
                    withHeader.complete(commaWithHeader)
                    advanceUntilIdle()
                    harness.state.draftTable shouldBe commaWithHeader
                    harness.state.draftSelection.delimiter shouldBe ','
                    harness.state.draftSelection.hasHeaderRow shouldBe true
                }
            }

            test("다시 나누는 중에 원래 구분 기준으로 되돌리면 그 기준으로 다시 나눠 끝난다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                    reader.suspends()
                    val back = reader.suspends()
                    val harness = opened("words.txt", reader, SettingsKind.Delimiter)

                    harness.send(ImportDeckIntent.SelectDelimiter(','))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.SelectDelimiter('\t'))
                    advanceUntilIdle()

                    harness.reader.calls
                        .last()
                        .delimiter shouldBe '\t'
                    val tab = ImportTestTables.text(rows = 2)
                    back.complete(tab)
                    advanceUntilIdle()
                    harness.state.draftTable shouldBe tab
                    harness.state.draftSelection.delimiter shouldBe '\t'
                    harness.state.isResplitting shouldBe false
                }
            }
        }

        context("시트의 첫 줄") {
            test("시트 화면의 첫 줄 체크는 덮개를 띄우고 그 시트를 다시 읽는다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(3, 2)))
                    val gate = reader.suspends()
                    val harness = opened("book.xlsx", reader, SettingsKind.Sheet)

                    harness.send(ImportDeckIntent.ChangeHeaderRow(false))
                    advanceUntilIdle()

                    harness.state.loading shouldBe ImportLoading.ReadingFile("book.xlsx")
                    val call =
                        harness.reader.calls
                            .last()
                            .shouldBeInstanceOf<ReadCall.FileRead>()
                    call.sheetIndex shouldBe 0
                    call.hasHeaderRow shouldBe false

                    val withoutHeader = ImportTestTables.xlsx(sheetRows = listOf(3, 2), hasHeaderRow = false)
                    gate.complete(withoutHeader)
                    advanceUntilIdle()

                    harness.state.loading shouldBe null
                    harness.state.draftTable shouldBe withoutHeader
                    harness.state.draftSelection.hasHeaderRow shouldBe false
                }
            }

            test("첫 줄 체크를 바꾼 뒤 다른 시트를 고르면 첫 줄 값을 비우고 새 시트의 판정으로 읽는다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader()
                            .returns(ImportTestTables.xlsx(sheetRows = listOf(3, 2)))
                            .returns(ImportTestTables.xlsx(sheetRows = listOf(3, 2), hasHeaderRow = false))
                            .returns(ImportTestTables.xlsx(sheetRows = listOf(3, 2), selectedSheet = 1))
                    val harness = opened("book.xlsx", reader, SettingsKind.Sheet)
                    harness.send(ImportDeckIntent.ChangeHeaderRow(false))
                    advanceUntilIdle()
                    harness.state.draftSelection.hasHeaderRow shouldBe false

                    harness.send(ImportDeckIntent.SelectSheet(1))
                    advanceUntilIdle()

                    val call =
                        harness.reader.calls
                            .last()
                            .shouldBeInstanceOf<ReadCall.FileRead>()
                    call.sheetIndex shouldBe 1
                    call.hasHeaderRow shouldBe null
                    harness.state.draftSelection.hasHeaderRow shouldBe null
                }
            }

            test("빈 시트를 고른 채 첫 줄 체크를 바꾸면 다시 읽지 않고 값만 바꾼다") {
                runTest(rig.dispatcher) {
                    val harness =
                        opened(
                            "book.xlsx",
                            FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(3, 0))),
                            SettingsKind.Sheet,
                        )
                    harness.send(ImportDeckIntent.SelectSheet(1))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ChangeHeaderRow(false))
                    advanceUntilIdle()

                    harness.reader.calls.size shouldBe 1
                    harness.state.draftSelection.hasHeaderRow shouldBe false
                    harness.state.settingsReason shouldBe SettingsReason.SheetEmpty
                }
            }
        }
    }

    private fun TestScope.opened(
        fileName: String,
        reader: FakeImportSourceReader,
        kind: SettingsKind,
        limits: ImportLimitsConfig = ImportLimitsConfig(),
    ): ImportViewModelHarness {
        val harness = pickedFile(rig, fileName, reader, limits)
        harness.send(ImportDeckIntent.ClickChangeSetting(kind))
        advanceUntilIdle()
        harness.state.step shouldBe ImportStep.Settings(kind, SettingsEntry.FromPreview)
        return harness
    }
}
