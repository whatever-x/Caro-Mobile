package com.whatever.caro.feature.importer

import app.cash.turbine.test
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.feature.importer.logic.ParseErrorTitle
import com.whatever.caro.feature.importer.logic.parseErrorCopy
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckSideEffect
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ForcedError
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.ReadCall
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** 선택기 결과 분류, 사본 복사, 첫 읽기 실패와 빈 결과를 확인한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportDeckViewModelReadTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        test("ClickPickFile은 선택기 열기를 요청하고 isPickerOpen을 true로 둔다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ClickPickFile)
                    awaitItem() shouldBe ImportDeckSideEffect.OpenFilePicker
                }

                harness.state.isPickerOpen shouldBe true
            }
        }

        test("선택기가 열려 있는 동안 ClickPickFile을 다시 보내도 열기를 한 번만 요청한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ClickPickFile)
                    harness.send(ImportDeckIntent.ClickPickFile)
                    awaitItem() shouldBe ImportDeckSideEffect.OpenFilePicker
                    advanceUntilIdle()
                    expectNoEvents()
                }
            }
        }

        test("ClickPickFile은 선택기를 열기 전에 caro-import에 남은 파일을 비운다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                harness.tempStorage.plantLeftover()

                harness.send(ImportDeckIntent.ClickPickFile)
                advanceUntilIdle()

                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("선택을 취소하면 isPickerOpen만 닫히고 나머지 상태는 처음 그대로다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                harness.send(ImportDeckIntent.ClickPickFile)
                advanceUntilIdle()
                harness.state.isPickerOpen shouldBe true

                harness.pickResult(PickResult.Cancelled)
                advanceUntilIdle()

                harness.state shouldBe ImportDeckState()
            }
        }

        test("지원하지 않는 확장자의 TooLarge 결과는 크기 안내가 아니라 지원하지 않는 파일 대화상자다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.pickResult(PickResult.TooLarge(displayName = "big.xls", sizeBytes = 200L * 1024 * 1024))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, "xls", isPaste = false)
                harness.state.step shouldBe ImportStep.Source
                harness.reader.calls.shouldBeEmpty()
            }
        }

        test("지원하는 확장자의 TooLarge 결과는 파일 크기 초과 대화상자다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.pickResult(PickResult.TooLarge(displayName = "big.apkg", sizeBytes = 200L * 1024 * 1024))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.FileTooLarge("apkg")
                harness.reader.calls.shouldBeEmpty()
            }
        }

        test("지원하지 않는 확장자는 읽지 않고 지원하지 않는 파일 대화상자를 띄우며 핸들을 정리한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                val file = FakePickedImportFile.ofSize("notes.docx", size = 100)

                harness.pick(file)
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.UNSUPPORTED_FILE, "docx", isPaste = false)
                harness.reader.calls.shouldBeEmpty()
                file.discardCount shouldBeGreaterThan 0
            }
        }

        test("선택기 실패는 파일을 읽지 못했다는 오류 대화상자를 띄운다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.pickResult(PickResult.Failed(IllegalStateException("copy failed")))
                advanceUntilIdle()

                val dialog = harness.state.dialog.shouldBeInstanceOf<ImportDialog.ParseError>()
                parseErrorCopy(dialog).title shouldBe ParseErrorTitle.ReadFailedFile
                harness.state.step shouldBe ImportStep.Source
            }
        }

        test("정확히 100MB인 apkg는 읽기를 시작한다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.apkgSummary("modern_anki21b_main.apkg"))
                val harness = rig.create(reader = reader)
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 100, reportedSize = 104_857_600L)

                harness.pick(file)
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 1
                harness.state.dialog shouldBe null
            }
        }

        test("100MB보다 1바이트 큰 apkg는 FileTooLarge 대화상자를 띄우고 읽지 않으며 핸들을 정리한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 100, reportedSize = 104_857_601L)

                harness.pick(file)
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.FileTooLarge("apkg")
                harness.reader.calls.shouldBeEmpty()
                file.discardCount shouldBeGreaterThan 0
            }
        }

        test("크기를 모르는 핸들이 복사 중 상한을 넘으면 FileTooLarge이고 부분 파일을 지우고 읽지 않는다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(limits = ImportLimitsConfig(maxFileBytes = 20_000L))
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 20_001, reportedSize = null)

                harness.pick(file)
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.FileTooLarge("apkg")
                harness.tempStorage.regularFiles().shouldBeEmpty()
                harness.reader.calls.shouldBeEmpty()
                file.discardCount shouldBeGreaterThan 0
            }
        }

        test("통과한 파일은 caro-import 아래 사본으로 복사해 그 경로와 파일 이름으로 읽는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                reader.suspends()
                val harness = rig.create(reader = reader)
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 20_000)

                harness.pick(file)
                advanceUntilIdle()

                val call =
                    harness.reader.calls
                        .single()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                call.path.parent shouldBe harness.tempStorage.root
                call.copiedBytes shouldBe file.content
                call.sourceName shouldBe "deck.apkg"
            }
        }

        test("제공자가 준 이름의 방향 제어 문자와 긴 이름은 정리한 뒤 확장자 판정과 읽기, 오버레이에 쓴다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                reader.suspends()
                val harness = rig.create(reader = reader)

                harness.pick(FakePickedImportFile.ofSize("deck\u202E.apkg", size = 100))
                advanceUntilIdle()

                harness.state.loading shouldBe ImportLoading.Parsing("deck.apkg")
                harness.reader.calls
                    .single()
                    .sourceName shouldBe "deck.apkg"
            }
        }

        test("아주 긴 이름도 확장자를 남겨 지원하는 파일로 읽는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                reader.suspends()
                val harness = rig.create(reader = reader)

                harness.pick(FakePickedImportFile.ofSize("a".repeat(5_000) + ".xlsx", size = 100))
                advanceUntilIdle()

                val sourceName =
                    harness.reader.calls
                        .single()
                        .sourceName
                sourceName.length shouldBe 120
                sourceName.endsWith(".xlsx") shouldBe true
            }
        }

        test("복사와 첫 읽기 동안 Parsing 오버레이에 파일 이름을 담고 선택기는 닫힌다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                reader.suspends()
                val harness = rig.create(reader = reader)
                harness.send(ImportDeckIntent.ClickPickFile)
                advanceUntilIdle()

                harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100))
                advanceUntilIdle()

                harness.state.loading shouldBe ImportLoading.Parsing("deck.apkg")
                harness.state.isPickerOpen shouldBe false
                harness.state.step shouldBe ImportStep.Source
            }
        }

        test("복사 중 뒤로 가기는 복사를 취소하고 부분 파일을 지운 뒤 핸들을 정리하고 Source로 간다") {
            runTest(rig.dispatcher) {
                // 복사 반복문은 중간에 양보하지 않으므로, Main을 즉시 실행으로 바꿔 읽기 도중 보낸 뒤로 가기가 바로 처리되게 한다.
                Dispatchers.setMain(UnconfinedTestDispatcher(rig.dispatcher.scheduler))
                val harness = rig.create(limits = ImportLimitsConfig(maxFileBytes = 1_000_000L))
                val file =
                    FakePickedImportFile.ofSize(
                        name = "deck.apkg",
                        size = 20_000,
                        onRead = { reads -> if (reads == 2) harness.send(ImportDeckIntent.ClickBack) },
                    )

                harness.pick(file)
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.Source
                harness.state.loading shouldBe null
                harness.tempStorage.regularFiles().shouldBeEmpty()
                harness.reader.calls.shouldBeEmpty()
                file.discardCount shouldBeGreaterThan 0
            }
        }

        test("첫 읽기 중 뒤로 가기는 읽기를 취소하고 임시 파일을 지운 뒤 Source로 가며 늦은 결과를 버린다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val gate = reader.suspends()
                val harness = rig.create(reader = reader)
                val file = FakePickedImportFile.ofSize("deck.apkg", size = 20_000)
                harness.pick(file)
                advanceUntilIdle()
                harness.tempStorage.regularFiles().size shouldBe 1

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                gate.complete(ImportTestTables.apkgSummary("modern_anki21b_main.apkg"))
                advanceUntilIdle()

                harness.state shouldBe ImportDeckState()
                harness.reader.cancelledReads shouldBe 1
                harness.tempStorage.regularFiles().shouldBeEmpty()
                file.discardCount shouldBeGreaterThan 0
            }
        }

        listOf(
            ImportParseErrorKind.CORRUPT_FILE to ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, "apkg", isPaste = false),
            ImportParseErrorKind.FILE_TOO_LARGE to ImportDialog.FileTooLarge("apkg"),
        ).forEach { (kind, expected) ->
            test("파서가 $kind 오류를 던지면 오류 종류와 확장자에 맞는 대화상자를 띄우고 임시 파일을 지운다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().fails(ImportParseException(kind, "forced"))
                    val harness = rig.create(reader = reader)

                    harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 20_000))
                    advanceUntilIdle()

                    harness.state.dialog shouldBe expected
                    harness.state.step shouldBe ImportStep.Source
                    harness.state.loading shouldBe null
                    harness.tempStorage.regularFiles().shouldBeEmpty()
                }
            }
        }

        test("Exception이 아닌 오류로 첫 읽기가 실패해도 읽기 실패 대화상자를 띄우고 오버레이를 걷으며 임시 파일을 지운다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().fails(ForcedError())
                val harness = rig.create(reader = reader)

                harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 20_000))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, "apkg", isPaste = false)
                harness.state.step shouldBe ImportStep.Source
                harness.state.loading shouldBe null
                harness.tempStorage.regularFiles().shouldBeEmpty()
            }
        }

        test("Exception이 아닌 오류로 다시 읽기가 실패해도 오버레이를 걷고 읽기 실패 대화상자를 띄운다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.text(sourceName = "words.txt")).fails(ForcedError())
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("words.txt", size = 100))
                advanceUntilIdle()

                harness.send(ImportDeckIntent.SelectDelimiter(','))
                advanceUntilIdle()

                harness.state.loading shouldBe null
                harness.state.pendingSelection shouldBe null
                harness.state.dialog.shouldBeInstanceOf<ImportDialog.ParseError>()
            }
        }

        test("첫 읽기 오류 대화상자를 닫으면 Source에 머문다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().fails(ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced"))
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100))
                advanceUntilIdle()
                harness.state.dialog.shouldNotBeNull()

                harness.send(ImportDeckIntent.DismissDialog)
                advanceUntilIdle()

                harness.state.dialog shouldBe null
                harness.state.step shouldBe ImportStep.Source
            }
        }

        test("하위 덱도 덱 없음 노트도 없는 apkg 요약은 EmptyResult 대화상자를 띄운다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.apkgSummary("empty_zero_notes.apkg"))
                val harness = rig.create(reader = reader)

                harness.pick(FakePickedImportFile.ofSize("empty.apkg", size = 100))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.EmptyResult(isPaste = false)
                harness.state.step shouldBe ImportStep.Source
                harness.state.loading shouldBe null
            }
        }

        test("EmptyResult 대화상자를 닫으면 Source에 머문다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.apkgSummary("empty_zero_notes.apkg"))
                val harness = rig.create(reader = reader)
                harness.pick(FakePickedImportFile.ofSize("empty.apkg", size = 100))
                advanceUntilIdle()
                harness.state.dialog shouldBe ImportDialog.EmptyResult(isPaste = false)

                harness.send(ImportDeckIntent.DismissDialog)
                advanceUntilIdle()

                harness.state.dialog shouldBe null
                harness.state.step shouldBe ImportStep.Source
            }
        }

        test("모든 시트의 rowCount가 0인 xlsx는 EmptyResult 대화상자를 띄운다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(0, 0)))
                val harness = rig.create(reader = reader)

                harness.pick(FakePickedImportFile.ofSize("book.xlsx", size = 100))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.EmptyResult(isPaste = false)
                harness.state.step shouldBe ImportStep.Source
            }
        }

        test("시트 하나만 비어 있는 xlsx는 EmptyResult 없이 SheetSelect로 간다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(3, 0)))
                val harness = rig.create(reader = reader)

                harness.pick(FakePickedImportFile.ofSize("book.xlsx", size = 100))
                advanceUntilIdle()

                harness.state.dialog shouldBe null
                harness.state.step shouldBe ImportStep.SheetSelect
            }
        }

        test("비어 있지 않은 줄이 없는 텍스트 파일은 EmptyResult 대화상자를 띄운다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.blankText(sourceName = "blank.txt"))
                val harness = rig.create(reader = reader)

                harness.pick(FakePickedImportFile.ofSize("blank.txt", size = 100))
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.EmptyResult(isPaste = false)
                harness.state.step shouldBe ImportStep.Source
            }
        }
    }
}
