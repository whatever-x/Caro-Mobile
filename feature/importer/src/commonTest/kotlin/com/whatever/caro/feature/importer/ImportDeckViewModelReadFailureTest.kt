package com.whatever.caro.feature.importer

import app.cash.turbine.test
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ForcedError
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 파일 첫 읽기의 실패: 읽기 전에 막은 파일, 파서 오류, 그 밖의 오류와 덮개 표시를 확인한다. */
class ImportDeckViewModelReadFailureTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        test("복사와 첫 읽기 동안 덮개에 정리한 파일 이름을 담고 선택기는 닫힌다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                reader.suspends()
                val harness = rig.create(reader = reader)
                harness.send(ImportDeckIntent.ClickPickFile)
                advanceUntilIdle()

                harness.pick(FakePickedImportFile.ofSize("deck‮.apkg", size = 100))
                advanceUntilIdle()

                harness.state.loading shouldBe ImportLoading.ReadingFile("deck.apkg")
                harness.state.isPickerOpen shouldBe false
                harness.state.step shouldBe ImportStep.Source
                harness.reader.calls
                    .single()
                    .sourceName shouldBe "deck.apkg"
            }
        }

        test("지원하지 않는 확장자는 읽지 않고 지원하지 않는 파일 대화상자를 띄우며 핸들을 정리한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                val file = FakePickedImportFile.ofSize("notes.docx", size = 100)

                harness.pick(file)
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, "docx", isPaste = false)
                harness.state.loading shouldBe null
                harness.reader.calls.shouldBeEmpty()
                file.discardCount shouldBeGreaterThan 0
            }
        }

        test("100MB보다 1바이트 큰 apkg는 크기 초과 대화상자를 띄우고 읽지 않는다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100, reportedSize = 104_857_601L))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.FileTooLarge("apkg")
                harness.state.step shouldBe ImportStep.Source
                harness.reader.calls.shouldBeEmpty()
            }
        }

        listOf(
            ImportParseErrorKind.CORRUPT_FILE to ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, "apkg", isPaste = false),
            ImportParseErrorKind.FILE_TOO_LARGE to ImportDialog.FileTooLarge("apkg"),
        ).forEach { (kind, expected) ->
            test("파서가 $kind 오류를 던지면 오류 종류와 확장자에 맞는 대화상자를 띄우고 임시 파일을 지운다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().fails(ImportParseException(kind, "forced")))

                    harness.pickNamed("deck.apkg")
                    advanceUntilIdle()

                    harness.state.dialog shouldBe expected
                    harness.state.step shouldBe ImportStep.Source
                    harness.state.loading shouldBe null
                    harness.tempStorage.regularFiles().shouldBeEmpty()
                }
            }
        }

        test("Exception이 아닌 오류로 첫 읽기가 실패해도 읽기 실패 대화상자를 띄우고 덮개를 걷는다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(reader = FakeImportSourceReader().fails(ForcedError()))

                harness.pickNamed("deck.apkg")
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, "apkg", isPaste = false)
                harness.state.loading shouldBe null
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("Anki 하위 덱 행 읽기가 실패하면 첫 읽기 실패와 같은 대화상자를 띄우고 Source로 간다") {
            runTest(rig.dispatcher) {
                val reader =
                    FakeImportSourceReader()
                        .returns(ImportTestTables.apkgSummary("modern_anki21b_main.apkg"))
                        .fails(ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced"))
                val harness = rig.create(reader = reader)

                harness.pickNamed("deck.apkg")
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, "apkg", isPaste = false)
                harness.state.step shouldBe ImportStep.Source
                harness.state.table shouldBe null
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        val oneRow = ImportTestTables.text(rows = 1)
        listOf(
            "미리보기를 만들다 Error가 나도" to oneRow.copy(rows = ThrowingList<ParsedRow> { ForcedError() }),
            "처리를 멈추지 않았는데 미리보기 계산 안에서 취소 예외가 나도" to oneRow.copy(rows = ThrowingList<ParsedRow> { CancellationException("forced") }),
            "Main에서 하는 기본 연결 계산이 실패해도" to oneRow.copy(noteTypes = ThrowingList<ParsedNoteType> { ForcedError() }),
        ).forEach { (cause, table) ->
            test("$cause 로딩 오버레이를 걷고 읽기 실패 대화상자를 띄운다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(table))

                    harness.pickNamed("words.txt")
                    advanceUntilIdle()

                    harness.state.dialog shouldBe
                        ImportDialog.ParseError(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, "txt", isPaste = false)
                    harness.state.loading shouldBe null
                    harness.state.step shouldBe ImportStep.Source
                    harness.tempStorage.regularFiles().shouldBeEmpty()
                }
            }
        }

        test("읽기 실패 대화상자는 뒤로 가기도 [확인]과 같아 닫기만 하고 Source에 머문다") {
            runTest(rig.dispatcher) {
                val harness =
                    rig.create(
                        reader = FakeImportSourceReader().fails(ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced")),
                    )
                harness.pickNamed("deck.apkg")
                advanceUntilIdle()
                harness.state.dialog.shouldNotBeNull()

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    expectNoEvents()
                }

                harness.state.dialog shouldBe null
                harness.state.step shouldBe ImportStep.Source
            }
        }
    }

    /** 원소를 꺼내는 순간 [error]가 만든 예외를 던진다. 판정은 목록 크기만 보므로 미리보기 계산까지 간다. */
    private class ThrowingList<T>(
        private val error: () -> Throwable,
    ) : AbstractList<T>() {
        override val size: Int = 1

        override fun get(index: Int): T = throw error()
    }
}
