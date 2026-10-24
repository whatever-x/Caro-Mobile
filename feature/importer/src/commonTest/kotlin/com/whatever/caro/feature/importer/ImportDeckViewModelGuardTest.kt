package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.platform.PickedImportFile
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.IOException
import okio.Source

/** 다음 단계로 못 가는 선택, 목록에 없는 선택 무시, 예상 밖 읽기 실패와 붙여넣기 다시 읽기 실패를 확인한다. */
class ImportDeckViewModelGuardTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val modernSummary by lazy { ImportTestTables.apkgSummary("modern_anki21b_main.apkg") }

    private class UnreadableFile(
        private val name: String,
    ) : PickedImportFile {
        var discardCount: Int = 0
            private set

        override fun displayName(): String = name

        override fun sizeBytes(): Long? = 100L

        override fun openSource(): Source = throw IOException("open failed")

        override fun discard() {
            discardCount += 1
        }
    }

    init {
        rig.installOn(this)

        test("고른 하위 덱이 하나도 없으면 [다음]을 막고 다시 읽지 않는다") {
            runTest(rig.dispatcher) {
                val harness = pickedApkg(FakeImportSourceReader().returns(modernSummary))
                harness.send(ImportDeckIntent.ToggleAllSubdecks)
                advanceUntilIdle()
                harness.state.selection.subdeckIds shouldBe ImportSelection().subdeckIds

                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 1
                harness.state.step shouldBe ImportStep.SubdeckSelect
                harness.state.loading shouldBe null
            }
        }

        test("목록에 없는 하위 덱 id의 토글은 무시한다") {
            runTest(rig.dispatcher) {
                val harness = pickedApkg(FakeImportSourceReader().returns(modernSummary))
                val before = harness.state.selection

                harness.send(ImportDeckIntent.ToggleSubdeck("unknown"))
                harness.send(ImportDeckIntent.ToggleSubdeck("none"))
                advanceUntilIdle()

                harness.state.selection shouldBe before
            }
        }

        test("고른 시트가 비어 있으면 [다음]을 막고 Mapping으로 가지 않는다") {
            runTest(rig.dispatcher) {
                val reader =
                    FakeImportSourceReader()
                        .returns(ImportTestTables.xlsx(sheetRows = listOf(3, 0)))
                        .returns(ImportTestTables.xlsx(sheetRows = listOf(3, 0), selectedSheet = 1))
                val harness = pickedFile("book.xlsx", reader)
                harness.send(ImportDeckIntent.SelectSheet(1))
                advanceUntilIdle()
                harness.state.selection.sheetIndex shouldBe 1

                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.SheetSelect
                harness.state.mappings.isEmpty() shouldBe true
            }
        }

        test("현재 시트나 목록에 없는 시트를 고르면 다시 읽지 않는다") {
            runTest(rig.dispatcher) {
                val harness = pickedFile("book.xlsx", FakeImportSourceReader().returns(ImportTestTables.xlsx()))

                harness.send(ImportDeckIntent.SelectSheet(0))
                harness.send(ImportDeckIntent.SelectSheet(9))
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 1
                harness.state.loading shouldBe null
            }
        }

        test("파서 밖의 예외로 읽기가 실패하면 파일을 읽지 못했다는 대화상자를 띄우고 임시 파일을 지운다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().fails(IllegalStateException("boom"))
                val harness = pickedFile("deck.apkg", reader)

                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, "apkg", isPaste = false)
                harness.state.loading shouldBe null
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("원본을 열지 못하면 읽지 않고 파일을 읽지 못했다는 대화상자를 띄우며 핸들을 정리한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                val file = UnreadableFile("deck.apkg")

                harness.pick(file)
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, "apkg", isPaste = false)
                harness.state.step shouldBe ImportStep.Source
                harness.state.loading shouldBe null
                harness.reader.calls.shouldBeEmpty()
                harness.tempStorage.regularFiles().shouldBeEmpty()
                file.discardCount shouldBeGreaterThan 0
            }
        }

        test("복사에 성공한 핸들도 읽기 전에 정리한다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(modernSummary)
                val harness = rig.create(reader = reader)
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 100)

                harness.pick(file)
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.SubdeckSelect
                file.discardCount shouldBe 1
            }
        }

        test("붙여넣기 텍스트 확인에서 다시 읽다 실패하면 확인 뒤 입력을 유지한 채 Paste로 돌아간다") {
            runTest(rig.dispatcher) {
                val name = ImportViewModelHarness.PASTE_SOURCE_NAME
                val reader =
                    FakeImportSourceReader()
                        .returns(ImportTestTables.text(sourceName = name))
                        .fails(ImportParseException(ImportParseErrorKind.TOO_MANY_COLUMNS, "forced"))
                val harness = rig.create(reader = reader)
                harness.pasteNext("a,b")
                advanceUntilIdle()
                harness.send(ImportDeckIntent.SelectDelimiter(','))
                advanceUntilIdle()
                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.TOO_MANY_COLUMNS, "", isPaste = true)

                harness.send(ImportDeckIntent.DismissDialog)
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.Paste
                harness.state.isPaste shouldBe true
                harness.state.pasteText shouldBe "a,b"
                harness.state.table shouldBe null
                harness.state.selection shouldBe ImportSelection()
                harness.state.dialog shouldBe null
            }
        }
    }

    private fun TestScope.pickedApkg(reader: FakeImportSourceReader): ImportViewModelHarness = pickedFile("deck.apkg", reader)

    private fun TestScope.pickedFile(
        fileName: String,
        reader: FakeImportSourceReader,
    ): ImportViewModelHarness {
        val harness = rig.create(reader = reader)
        harness.pick(FakePickedImportFile.ofSize(fileName, size = 100))
        advanceUntilIdle()
        return harness
    }
}
