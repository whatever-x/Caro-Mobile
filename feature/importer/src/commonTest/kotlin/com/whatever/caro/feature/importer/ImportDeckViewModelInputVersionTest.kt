package com.whatever.caro.feature.importer

import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 화면이 단계별 보존 상태의 키로 쓰는 sourceVersion과 mappingInputVersion이 언제 바뀌는지 확인한다. */
class ImportDeckViewModelInputVersionTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val modernSummary by lazy { ImportTestTables.apkgSummary("modern_anki21b_main.apkg") }

    init {
        rig.installOn(this)

        test("새 자료를 처음 읽으면 두 버전이 모두 새 값이 된다") {
            runTest(rig.dispatcher) {
                val harness = pickedXlsx(FakeImportSourceReader().returns(ImportTestTables.xlsx()))

                harness.state.sourceVersion shouldNotBe 0
                harness.state.mappingInputVersion shouldNotBe 0
            }
        }

        test("시트를 바꿔 다시 읽으면 sourceVersion은 그대로이고 mappingInputVersion만 바뀐다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx()).returns(ImportTestTables.xlsx(selectedSheet = 1))
                val harness = pickedXlsx(reader)
                val before = harness.state

                harness.send(ImportDeckIntent.SelectSheet(1))
                advanceUntilIdle()

                harness.state.sourceVersion shouldBe before.sourceVersion
                harness.state.mappingInputVersion shouldNotBe before.mappingInputVersion
            }
        }

        test("제목 행을 바꿔 다시 읽어도 sourceVersion은 그대로다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx()).returns(ImportTestTables.xlsx(hasHeaderRow = false))
                val harness = pickedXlsx(reader)
                val before = harness.state.sourceVersion

                harness.send(ImportDeckIntent.ChangeHeaderRow(hasHeaderRow = false))
                advanceUntilIdle()

                harness.state.sourceVersion shouldBe before
            }
        }

        test("구분자를 바꿔 다시 읽어도 sourceVersion은 그대로이고 mappingInputVersion만 바뀐다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.text()).returns(ImportTestTables.text(delimiter = ","))
                val harness = pickedText(reader)
                val before = harness.state

                harness.send(ImportDeckIntent.SelectDelimiter(','))
                advanceUntilIdle()

                harness.state.sourceVersion shouldBe before.sourceVersion
                harness.state.mappingInputVersion shouldNotBe before.mappingInputVersion
            }
        }

        test("하위 덱을 골라 다시 읽으면 sourceVersion은 그대로이고 mappingInputVersion만 바뀐다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(modernSummary).returns(ImportTestTables.withOneRow(modernSummary))
                val harness = pickedApkg(reader)
                val before = harness.state

                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.Mapping
                harness.state.sourceVersion shouldBe before.sourceVersion
                harness.state.mappingInputVersion shouldNotBe before.mappingInputVersion
            }
        }

        test("읽은 표를 그대로 두고 항목 연결에 들어가면 mappingInputVersion은 그대로다") {
            runTest(rig.dispatcher) {
                val harness = pickedXlsx(FakeImportSourceReader().returns(ImportTestTables.xlsx()))
                val before = harness.state

                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.Mapping
                harness.state.sourceVersion shouldBe before.sourceVersion
                harness.state.mappingInputVersion shouldBe before.mappingInputVersion
            }
        }

        test("항목 연결에서 돌아가 그대로 다시 들어가도 두 버전은 그대로다") {
            runTest(rig.dispatcher) {
                val harness = pickedXlsx(FakeImportSourceReader().returns(ImportTestTables.xlsx()))
                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()
                val before = harness.state

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                harness.state.sourceVersion shouldBe before.sourceVersion
                harness.state.mappingInputVersion shouldBe before.mappingInputVersion
            }
        }

        test("읽은 내용을 버리고 새 자료를 읽으면 이전에 쓴 버전과 겹치지 않는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx()).returns(ImportTestTables.xlsx())
                val harness = pickedXlsx(reader)
                val firstSource = harness.state.sourceVersion
                val firstMapping = harness.state.mappingInputVersion
                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                harness.send(ImportDeckIntent.ConfirmDiscard)
                advanceUntilIdle()
                harness.state.step shouldBe ImportStep.Source

                harness.pick(FakePickedImportFile.ofSize("book.xlsx", size = 100))
                advanceUntilIdle()

                harness.state.sourceVersion shouldNotBe firstSource
                harness.state.mappingInputVersion shouldNotBe firstMapping
            }
        }
    }

    private fun TestScope.pickedXlsx(reader: FakeImportSourceReader): ImportViewModelHarness = picked(reader, "book.xlsx")

    private fun TestScope.pickedText(reader: FakeImportSourceReader): ImportViewModelHarness = picked(reader, "words.txt")

    private fun TestScope.pickedApkg(reader: FakeImportSourceReader): ImportViewModelHarness = picked(reader, "deck.apkg")

    private fun TestScope.picked(
        reader: FakeImportSourceReader,
        fileName: String,
    ): ImportViewModelHarness {
        val harness = rig.create(reader = reader)
        harness.pick(FakePickedImportFile.ofSize(fileName, size = 100))
        advanceUntilIdle()
        return harness
    }
}
