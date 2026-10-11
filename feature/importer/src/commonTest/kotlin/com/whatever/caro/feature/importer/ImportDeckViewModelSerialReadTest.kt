package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 취소에 바로 응하지 않는 이전 읽기가 남아 있어도 읽기가 겹쳐 돌지 않고, 그 읽기가 쓰던 임시 사본은 읽기가 끝난 뒤에 지운다. */
class ImportDeckViewModelSerialReadTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        test("뒤로 간 뒤 다시 시작한 읽기는 취소에 응하지 않는 이전 읽기가 끝나야 시작한다") {
            runTest(rig.dispatcher) {
                val pasted = ImportTestTables.text(sourceName = ImportViewModelHarness.PASTE_SOURCE_NAME)
                val reader = FakeImportSourceReader()
                val firstGate = reader.suspendsIgnoringCancellation()
                reader.returns(pasted)
                val harness = rig.create(reader = reader)
                harness.pasteNext("a\tb")
                advanceUntilIdle()
                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                harness.state.step shouldBe ImportStep.Paste

                harness.send(ImportDeckIntent.ClickPasteNext(ImportViewModelHarness.PASTE_SOURCE_NAME, SpreadsheetDateStyle.YEAR_MONTH_DAY))
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 1

                firstGate.complete(pasted)
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 2
                harness.state.step shouldBe ImportStep.TextCheck
            }
        }

        test("읽기를 취소해도 그 읽기가 끝나기 전에는 임시 사본을 지우지 않고 끝나면 지운다") {
            runTest(rig.dispatcher) {
                val table = ImportTestTables.text()
                val reader = FakeImportSourceReader()
                val gate = reader.suspendsIgnoringCancellation()
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                advanceUntilIdle()
                harness.reader.calls.size shouldBe 1
                harness.tempStorage.regularFiles().size shouldBe 1

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()

                harness.tempStorage.regularFiles().size shouldBe 1

                gate.complete(table)
                advanceUntilIdle()

                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }
    }
}
