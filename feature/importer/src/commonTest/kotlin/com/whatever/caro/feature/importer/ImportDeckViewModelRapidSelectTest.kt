package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
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
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** 텍스트를 다시 나누는 중에 다른 컨트롤을 바꿔도 먼저 고른 값이 사라지지 않는지 확인한다. */
class ImportDeckViewModelRapidSelectTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        test("구분자를 읽는 중에 제목 행을 바꾸면 고른 구분자를 유지한 채 마지막 선택으로 읽는다") {
            runTest(rig.dispatcher) {
                val finalTable = ImportTestTables.text(rows = 6, delimiter = ";", hasHeaderRow = true)
                val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                reader.suspends()
                reader.returns(finalTable)
                val harness = pickedText(reader)
                harness.send(ImportDeckIntent.SelectDelimiter(';'))
                runCurrent()

                harness.send(ImportDeckIntent.ChangeHeaderRow(hasHeaderRow = true))
                advanceUntilIdle()

                val last =
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                last.delimiter shouldBe ';'
                last.hasHeaderRow shouldBe true
                harness.reader.cancelledReads shouldBe 1
                harness.state.selection shouldBe ImportSelection(delimiter = ';', hasHeaderRow = true)
                harness.state.pendingSelection shouldBe null
                harness.state.table shouldBe finalTable
                harness.state.loading shouldBe null
            }
        }

        test("제목 행을 읽는 중에 구분자를 바꿔도 고른 제목 행을 유지한다") {
            runTest(rig.dispatcher) {
                val finalTable = ImportTestTables.text(rows = 6, delimiter = ",", hasHeaderRow = true)
                val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                reader.suspends()
                reader.returns(finalTable)
                val harness = pickedText(reader)
                harness.send(ImportDeckIntent.ChangeHeaderRow(hasHeaderRow = true))
                runCurrent()

                harness.send(ImportDeckIntent.SelectDelimiter(','))
                advanceUntilIdle()

                harness.state.selection shouldBe ImportSelection(delimiter = ',', hasHeaderRow = true)
                harness.state.table shouldBe finalTable
            }
        }

        test("두 선택을 읽기가 시작되기 전에 연달아 보내도 둘 다 반영해 한 번만 읽는다") {
            runTest(rig.dispatcher) {
                val finalTable = ImportTestTables.text(rows = 6, delimiter = ";", hasHeaderRow = true)
                val reader = FakeImportSourceReader().returns(ImportTestTables.text()).returns(finalTable)
                val harness = pickedText(reader)

                harness.send(ImportDeckIntent.SelectDelimiter(';'))
                harness.send(ImportDeckIntent.ChangeHeaderRow(hasHeaderRow = true))
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 2
                val last =
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                last.delimiter shouldBe ';'
                last.hasHeaderRow shouldBe true
                harness.state.selection shouldBe ImportSelection(delimiter = ';', hasHeaderRow = true)
            }
        }

        test("다시 읽는 동안 사용자가 누른 선택을 pendingSelection으로 보이고 확정한 선택은 그대로 둔다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                reader.suspends()
                val harness = pickedText(reader)

                harness.send(ImportDeckIntent.SelectDelimiter(';'))
                runCurrent()

                harness.state.loading shouldBe ImportLoading.TextReparsing
                harness.state.pendingSelection shouldBe ImportSelection(delimiter = ';')
                harness.state.selection shouldBe ImportSelection()
            }
        }

        test("읽는 중 뒤로 가기는 pendingSelection을 비우고 이어서 바꾸는 선택도 확정한 선택에서 시작한다") {
            runTest(rig.dispatcher) {
                val finalTable = ImportTestTables.text(rows = 6, delimiter = ",")
                val reader = FakeImportSourceReader().returns(ImportTestTables.text())
                reader.suspends()
                reader.returns(finalTable)
                val harness = pickedText(reader)
                harness.send(ImportDeckIntent.SelectDelimiter(';'))
                runCurrent()

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()

                harness.state.pendingSelection shouldBe null
                harness.state.loading shouldBe null
                harness.state.selection shouldBe ImportSelection()

                harness.send(ImportDeckIntent.SelectDelimiter(','))
                advanceUntilIdle()

                val last =
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                last.delimiter shouldBe ','
                last.hasHeaderRow shouldBe null
            }
        }

        test("다시 읽기가 실패하면 pendingSelection을 비운다") {
            runTest(rig.dispatcher) {
                val reader =
                    FakeImportSourceReader()
                        .returns(ImportTestTables.text())
                        .fails(ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced"))
                val harness = pickedText(reader)

                harness.send(ImportDeckIntent.SelectDelimiter(';'))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, "txt", isPaste = false)
                harness.state.pendingSelection shouldBe null
                harness.state.step shouldBe ImportStep.TextCheck
            }
        }
    }

    private fun TestScope.pickedText(reader: FakeImportSourceReader): ImportViewModelHarness {
        val harness = rig.create(reader = reader)
        harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
        advanceUntilIdle()
        return harness
    }
}
