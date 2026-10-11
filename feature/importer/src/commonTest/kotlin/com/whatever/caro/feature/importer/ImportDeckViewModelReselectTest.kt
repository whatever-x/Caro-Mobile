package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.ReadCall
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** xlsx 시트와 제목 행, 텍스트 확인의 구분자와 제목 행을 바꿔 다시 읽는 흐름을 확인한다. */
class ImportDeckViewModelReselectTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        test("xlsx 첫 읽기는 SheetSelect로 가고 제목 행 기본값을 layout에서 가져온다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(reader = rig.realParserReader())

                harness.pick(FakePickedImportFile.fromFixture("xlsx/wordlist_multi_sheet.xlsx"))
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.SheetSelect
                harness.state.table
                    ?.sheets
                    ?.size shouldBe 3
                harness.state.selection.sheetIndex shouldBe 0
                harness.state.selection.hasHeaderRow shouldBe true
                harness.reader.calls
                    .single()
                    .hasHeaderRow shouldBe null
            }
        }

        test("SelectSheet는 hasHeaderRow를 null로 해 고른 시트를 다시 읽고 성공하면 확정한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(reader = rig.realParserReader())
                harness.pick(FakePickedImportFile.fromFixture("xlsx/wordlist_multi_sheet.xlsx"))
                advanceUntilIdle()

                harness.send(ImportDeckIntent.SelectSheet(1))
                advanceUntilIdle()

                val second =
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                second.sheetIndex shouldBe 1
                second.hasHeaderRow shouldBe null
                harness.state.selection.sheetIndex shouldBe 1
                harness.state.table
                    ?.noteTypes
                    ?.single()
                    ?.name shouldBe "Day2"
                harness.state.selection.hasHeaderRow shouldBe
                    harness.state.table
                        ?.layout
                        ?.hasHeaderRow
                harness.state.loading shouldBe null
                harness.state.step shouldBe ImportStep.SheetSelect
            }
        }

        test("첫 줄은 제목을 바꾸면 지정한 값으로 같은 시트를 다시 읽는다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(reader = rig.realParserReader())
                harness.pick(FakePickedImportFile.fromFixture("xlsx/wordlist_multi_sheet.xlsx"))
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ChangeHeaderRow(hasHeaderRow = false))
                advanceUntilIdle()

                val second =
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                second.sheetIndex shouldBe 0
                second.hasHeaderRow shouldBe false
                harness.state.selection.hasHeaderRow shouldBe false
                harness.state.loading shouldBe null
            }
        }

        test("시트를 바꾼 뒤 제목 행을 바꾸면 바뀐 시트로 다시 읽는다") {
            runTest(rig.dispatcher) {
                val reader =
                    FakeImportSourceReader()
                        .returns(ImportTestTables.xlsx())
                        .returns(ImportTestTables.xlsx(selectedSheet = 1))
                        .returns(ImportTestTables.xlsx(selectedSheet = 1, hasHeaderRow = false))
                val harness = pickedXlsx(reader)
                harness.send(ImportDeckIntent.SelectSheet(1))
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ChangeHeaderRow(hasHeaderRow = false))
                advanceUntilIdle()

                val third =
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                third.sheetIndex shouldBe 1
                third.hasHeaderRow shouldBe false
            }
        }

        test("시트를 다시 읽는 동안 Reparsing 오버레이를 보여주고 이전 표를 유지한다") {
            runTest(rig.dispatcher) {
                val first = ImportTestTables.xlsx()
                val reader = FakeImportSourceReader().returns(first)
                reader.suspends()
                val harness = pickedXlsx(reader)

                harness.send(ImportDeckIntent.SelectSheet(1))
                advanceUntilIdle()

                harness.state.loading shouldBe ImportLoading.Reparsing("book.xlsx")
                harness.state.table shouldBe first
                harness.state.selection.sheetIndex shouldBe 0
            }
        }

        test("시트를 다시 읽는 중 뒤로 가기는 읽기를 취소하고 바꾸기 전 시트와 제목 행으로 되돌린다") {
            runTest(rig.dispatcher) {
                val first = ImportTestTables.xlsx(hasHeaderRow = true)
                val reader = FakeImportSourceReader().returns(first)
                val gate = reader.suspends()
                val harness = pickedXlsx(reader)
                val selectionBefore = harness.state.selection
                harness.send(ImportDeckIntent.SelectSheet(1))
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                gate.complete(ImportTestTables.xlsx(selectedSheet = 1, hasHeaderRow = false))
                advanceUntilIdle()

                selectionBefore.sheetIndex shouldBe 0
                harness.state.selection shouldBe selectionBefore
                harness.state.table shouldBe first
                harness.state.step shouldBe ImportStep.SheetSelect
                harness.state.loading shouldBe null
                harness.state.dialog shouldBe null
                harness.reader.cancelledReads shouldBe 1
            }
        }

        test("제목 행을 다시 읽는 중 뒤로 가기는 읽기를 취소하고 바꾸기 전 제목 행으로 되돌린다") {
            runTest(rig.dispatcher) {
                val first = ImportTestTables.xlsx(hasHeaderRow = true)
                val reader = FakeImportSourceReader().returns(first)
                val gate = reader.suspends()
                val harness = pickedXlsx(reader)
                val selectionBefore = harness.state.selection
                harness.send(ImportDeckIntent.ChangeHeaderRow(hasHeaderRow = false))
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                gate.complete(ImportTestTables.xlsx(hasHeaderRow = false))
                advanceUntilIdle()

                harness.state.selection shouldBe selectionBefore
                harness.state.table shouldBe first
                harness.state.step shouldBe ImportStep.SheetSelect
                harness.reader.cancelledReads shouldBe 1
            }
        }

        test("시트를 다시 읽다 실패하면 ParseError를 띄우고 확인하면 표와 임시 파일을 지우고 Source로 간다") {
            runTest(rig.dispatcher) {
                val reader =
                    FakeImportSourceReader()
                        .returns(ImportTestTables.xlsx())
                        .fails(ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced"))
                val harness = pickedXlsx(reader)
                harness.send(ImportDeckIntent.SelectSheet(1))
                advanceUntilIdle()
                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, "xlsx", isPaste = false)

                harness.send(ImportDeckIntent.DismissDialog)
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.Source
                harness.state.table shouldBe null
                harness.state.selection shouldBe ImportSelection()
                harness.state.dialog shouldBe null
                harness.state.loading shouldBe null
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("xlsx의 [다음]은 다시 읽지 않고 기본 연결로 Mapping에 간다") {
            runTest(rig.dispatcher) {
                val harness = pickedXlsx(FakeImportSourceReader().returns(ImportTestTables.xlsx()))

                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 1
                harness.state.step shouldBe ImportStep.Mapping
                harness.state.mappings.keys shouldBe setOf("xlsx")
                harness.state.mappings["xlsx"] shouldBe
                    FieldMapping(
                        noteTypeId = "xlsx",
                        targets = mapOf(FieldMapping.FRONT to listOf("열 1"), FieldMapping.BACK to listOf("열 2")),
                    )
            }
        }

        test("텍스트 파일 첫 읽기는 TextCheck로 간다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(reader = rig.realParserReader())

                harness.pick(FakePickedImportFile.fromFixture("text/tsv_no_header.tsv"))
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.TextCheck
                harness.state.isPaste shouldBe false
                harness.state.table
                    ?.format shouldBe ImportFormat.TEXT
            }
        }

        test("SelectDelimiter는 결과 표 자리에만 TextReparsing을 보여주고 고른 구분자로 파일을 다시 읽는다") {
            runTest(rig.dispatcher) {
                val first = ImportTestTables.text(rows = 3)
                val reread = ImportTestTables.text(rows = 5, delimiter = ",")
                val reader = FakeImportSourceReader().returns(first)
                val gate = reader.suspends()
                val harness = pickedText(reader)

                harness.send(ImportDeckIntent.SelectDelimiter(','))
                advanceUntilIdle()

                val call =
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                call.delimiter shouldBe ','
                call.hasHeaderRow shouldBe null
                harness.state.loading shouldBe ImportLoading.TextReparsing
                harness.state.overlayLoading shouldBe null
                harness.state.table shouldBe first

                gate.complete(reread)
                advanceUntilIdle()

                harness.state.table shouldBe reread
                harness.state.selection.delimiter shouldBe ','
                harness.state.loading shouldBe null
                harness.state.step shouldBe ImportStep.TextCheck
            }
        }

        test("구분자를 고른 뒤 제목 행을 바꾸면 고른 구분자를 유지한 채 다시 읽고 dateStyle도 그대로다") {
            runTest(rig.dispatcher) {
                val reader =
                    FakeImportSourceReader()
                        .returns(ImportTestTables.text())
                        .returns(ImportTestTables.text(delimiter = ","))
                        .returns(ImportTestTables.text(delimiter = ",", hasHeaderRow = true))
                val harness = pickedText(reader, dateStyle = SpreadsheetDateStyle.MONTH_DAY_YEAR)
                harness.send(ImportDeckIntent.SelectDelimiter(','))
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ChangeHeaderRow(hasHeaderRow = true))
                advanceUntilIdle()

                val third =
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                third.delimiter shouldBe ','
                third.hasHeaderRow shouldBe true
                harness.reader.calls
                    .map { call -> call.dateStyle }
                    .toSet() shouldBe setOf(SpreadsheetDateStyle.MONTH_DAY_YEAR)
                harness.state.selection.delimiter shouldBe ','
                harness.state.selection.hasHeaderRow shouldBe true
            }
        }

        test("구분자를 빠르게 두 번 바꾸면 늦게 끝난 첫 결과를 버리고 마지막 결과만 남긴다") {
            runTest(rig.dispatcher) {
                val first = ImportTestTables.text(rows = 3)
                val commaResult = ImportTestTables.text(rows = 5, delimiter = ",")
                val semicolonResult = ImportTestTables.text(rows = 7, delimiter = ";")
                val reader = FakeImportSourceReader().returns(first)
                val commaGate = reader.suspends()
                val semicolonGate = reader.suspends()
                val harness = pickedText(reader)
                harness.send(ImportDeckIntent.SelectDelimiter(','))
                advanceUntilIdle()
                harness.send(ImportDeckIntent.SelectDelimiter(';'))
                advanceUntilIdle()

                commaGate.complete(commaResult)
                advanceUntilIdle()

                harness.state.loading shouldBe ImportLoading.TextReparsing
                harness.state.table shouldBe first
                harness.state.selection.delimiter shouldBe null

                semicolonGate.complete(semicolonResult)
                advanceUntilIdle()

                harness.state.table shouldBe semicolonResult
                harness.state.selection.delimiter shouldBe ';'
                harness.state.loading shouldBe null
            }
        }

        test("텍스트를 다시 나누는 중 뒤로 가기는 읽기를 취소하고 바꾸기 전 구분자와 표로 되돌린다") {
            runTest(rig.dispatcher) {
                val semicolonResult = ImportTestTables.text(rows = 4, delimiter = ";")
                val reader = FakeImportSourceReader().returns(ImportTestTables.text()).returns(semicolonResult)
                val gate = reader.suspends()
                val harness = pickedText(reader)
                harness.send(ImportDeckIntent.SelectDelimiter(';'))
                advanceUntilIdle()
                harness.send(ImportDeckIntent.SelectDelimiter(','))
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                gate.complete(ImportTestTables.text(rows = 9, delimiter = ","))
                advanceUntilIdle()

                harness.state.selection.delimiter shouldBe ';'
                harness.state.table shouldBe semicolonResult
                harness.state.step shouldBe ImportStep.TextCheck
                harness.state.loading shouldBe null
                harness.state.dialog shouldBe null
                harness.reader.cancelledReads shouldBe 1
            }
        }

        test("붙여넣기의 구분자 변경은 파일이 아니라 붙여 넣은 글을 같은 sourceName으로 다시 읽는다") {
            runTest(rig.dispatcher) {
                val name = ImportViewModelHarness.PASTE_SOURCE_NAME
                val reader =
                    FakeImportSourceReader()
                        .returns(ImportTestTables.text(sourceName = name))
                        .returns(ImportTestTables.text(sourceName = name, rows = 2, delimiter = ";"))
                val harness = rig.create(reader = reader)
                harness.pasteNext("a;b")
                advanceUntilIdle()

                harness.send(ImportDeckIntent.SelectDelimiter(';'))
                advanceUntilIdle()

                val second =
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.TextRead>()
                second.text shouldBe "a;b"
                second.sourceName shouldBe name
                second.delimiter shouldBe ';'
                harness.state.selection.delimiter shouldBe ';'
            }
        }

        test("텍스트 확인의 [다음]은 다시 읽지 않고 기본 연결로 Mapping에 간다") {
            runTest(rig.dispatcher) {
                val harness = pickedText(FakeImportSourceReader().returns(ImportTestTables.text()))

                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 1
                harness.state.step shouldBe ImportStep.Mapping
                harness.state.mappings.keys shouldBe setOf("text")
            }
        }
    }

    private fun TestScope.pickedXlsx(reader: FakeImportSourceReader): ImportViewModelHarness {
        val harness = rig.create(reader = reader)
        harness.pick(FakePickedImportFile.ofSize("book.xlsx", size = 100))
        advanceUntilIdle()
        return harness
    }

    private fun TestScope.pickedText(
        reader: FakeImportSourceReader,
        dateStyle: SpreadsheetDateStyle = SpreadsheetDateStyle.YEAR_MONTH_DAY,
    ): ImportViewModelHarness {
        val harness = rig.create(reader = reader)
        harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100), dateStyle)
        advanceUntilIdle()
        return harness
    }
}
