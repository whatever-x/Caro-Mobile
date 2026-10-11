package com.whatever.caro.feature.importer

import app.cash.turbine.test
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.ReadCall
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 붙여넣기 입력: 길이 상한, 첫 읽기, 읽기 실패와 빈 결과 처리, 뒤로 가기를 확인한다. */
class ImportDeckViewModelPasteTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val limits = ImportLimitsConfig(maxPasteChars = 10)

    init {
        rig.installOn(this)

        test("ClickPaste는 Paste 단계로 간다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()

                harness.send(ImportDeckIntent.ClickPaste)
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.Paste
            }
        }

        test("UpdatePasteText는 상한을 한 글자 넘는 글은 자르지 않고 보관한다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(limits = limits)
                harness.send(ImportDeckIntent.ClickPaste)

                harness.send(ImportDeckIntent.UpdatePasteText(OVER_LIMIT_TEXT))
                advanceUntilIdle()

                harness.state.pasteText shouldBe OVER_LIMIT_TEXT
            }
        }

        test("UpdatePasteText는 상한을 크게 넘는 글을 상한 + 1자까지만 보관하고 [다음]을 막는다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(limits = limits)
                harness.send(ImportDeckIntent.ClickPaste)

                harness.send(ImportDeckIntent.UpdatePasteText("x".repeat(1_000)))
                harness.send(ImportDeckIntent.ClickPasteNext(ImportViewModelHarness.PASTE_SOURCE_NAME, SpreadsheetDateStyle.YEAR_MONTH_DAY))
                advanceUntilIdle()

                harness.state.pasteText shouldBe "x".repeat(11)
                harness.state.step shouldBe ImportStep.Paste
                harness.reader.calls.shouldBeEmpty()
            }
        }

        test("상한 바로 뒤가 서로게이트 쌍 가운데여도 보관한 글은 상한을 넘어 [다음]이 막힌다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(limits = limits)
                harness.send(ImportDeckIntent.ClickPaste)

                harness.send(ImportDeckIntent.UpdatePasteText(AT_LIMIT_TEXT + "\uD83D\uDE00\uD83D\uDE00"))
                harness.send(ImportDeckIntent.ClickPasteNext(ImportViewModelHarness.PASTE_SOURCE_NAME, SpreadsheetDateStyle.YEAR_MONTH_DAY))
                advanceUntilIdle()

                harness.state.pasteText.length shouldBe 11
                harness.reader.calls.shouldBeEmpty()
            }
        }

        test("비어 있는 붙여넣기는 [다음]을 막는다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(limits = limits)
                harness.send(ImportDeckIntent.ClickPaste)

                harness.send(ImportDeckIntent.ClickPasteNext(ImportViewModelHarness.PASTE_SOURCE_NAME, SpreadsheetDateStyle.YEAR_MONTH_DAY))
                advanceUntilIdle()

                harness.reader.calls.shouldBeEmpty()
                harness.state.step shouldBe ImportStep.Paste
                harness.state.loading shouldBe null
            }
        }

        test("상한을 한 글자 넘는 붙여넣기는 [다음]을 막고 글을 자르지 않는다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(limits = limits)

                harness.pasteNext(OVER_LIMIT_TEXT)
                advanceUntilIdle()

                harness.reader.calls.shouldBeEmpty()
                harness.state.step shouldBe ImportStep.Paste
                harness.state.pasteText shouldBe OVER_LIMIT_TEXT
            }
        }

        test("상한 길이 그대로의 붙여넣기는 읽는다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(ImportTestTables.text(sourceName = ImportViewModelHarness.PASTE_SOURCE_NAME))
                val harness = rig.create(reader = reader, limits = limits)

                harness.pasteNext(AT_LIMIT_TEXT)
                advanceUntilIdle()

                harness.reader.calls.size shouldBe 1
            }
        }

        test("ClickPasteNext는 넘겨받은 sourceName과 dateStyle로 글을 읽고 파일 이름 없는 Parsing 오버레이를 보인다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                reader.suspends()
                val harness = rig.create(reader = reader)

                harness.pasteNext("a\tb", sourceName = "Pasted cards", dateStyle = SpreadsheetDateStyle.MONTH_DAY_YEAR)
                advanceUntilIdle()

                val call =
                    harness.reader.calls
                        .single()
                        .shouldBeInstanceOf<ReadCall.TextRead>()
                call.text shouldBe "a\tb"
                call.sourceName shouldBe "Pasted cards"
                call.dateStyle shouldBe SpreadsheetDateStyle.MONTH_DAY_YEAR
                call.delimiter shouldBe null
                call.hasHeaderRow shouldBe null
                harness.state.loading shouldBe ImportLoading.Parsing(fileName = null)
            }
        }

        test("붙여넣기 읽기가 끝나면 TextCheck로 가고 붙여넣기 원본으로 표시한다") {
            runTest(rig.dispatcher) {
                val table = ImportTestTables.text(sourceName = ImportViewModelHarness.PASTE_SOURCE_NAME)
                val harness = rig.create(reader = FakeImportSourceReader().returns(table))

                harness.pasteNext("a\tb")
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.TextCheck
                harness.state.isPaste shouldBe true
                harness.state.table shouldBe table
                harness.state.loading shouldBe null
            }
        }

        test("제목 줄만 있는 붙여넣기는 EmptyResult 없이 TextCheck로 간다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.titleOnlyText()))

                harness.pasteNext("a\tb")
                advanceUntilIdle()

                harness.state.dialog shouldBe null
                harness.state.step shouldBe ImportStep.TextCheck
            }
        }

        test("노트 유형이 없는 붙여넣기는 EmptyResult를 띄우고 글을 유지한 채 Paste에 머문다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.blankText()))

                harness.pasteNext("\n\n")
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.EmptyResult(isPaste = true)
                harness.state.step shouldBe ImportStep.Paste
                harness.state.pasteText shouldBe "\n\n"
            }
        }

        test("붙여넣기 EmptyResult를 닫아도 Paste에 머물고 글이 남는다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.blankText()))
                harness.pasteNext("\n\n")
                advanceUntilIdle()

                harness.send(ImportDeckIntent.DismissDialog)
                advanceUntilIdle()

                harness.state.dialog shouldBe null
                harness.state.step shouldBe ImportStep.Paste
                harness.state.pasteText shouldBe "\n\n"
            }
        }

        test("붙여넣기 읽기가 실패하면 붙여넣기 오류 대화상자를 띄우고 글을 유지한 채 Paste에 머문다") {
            runTest(rig.dispatcher) {
                val error = ImportParseException(ImportParseErrorKind.TOO_MANY_COLUMNS, "forced")
                val harness = rig.create(reader = FakeImportSourceReader().fails(error))

                harness.pasteNext("a\tb")
                advanceUntilIdle()

                harness.state.dialog shouldBe ImportDialog.ParseError(ImportParseErrorKind.TOO_MANY_COLUMNS, extension = "", isPaste = true)
                harness.state.step shouldBe ImportStep.Paste
                harness.state.pasteText shouldBe "a\tb"
                harness.state.loading shouldBe null
            }
        }

        test("붙여넣기 첫 읽기 중 뒤로 가기는 읽기를 취소하고 입력을 유지한 채 Paste로 간다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader()
                val gate = reader.suspends()
                val harness = rig.create(reader = reader)
                harness.pasteNext("a\tb")
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                gate.complete(ImportTestTables.text(sourceName = ImportViewModelHarness.PASTE_SOURCE_NAME))
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.Paste
                harness.state.loading shouldBe null
                harness.state.pasteText shouldBe "a\tb"
                harness.state.table shouldBe null
                harness.reader.cancelledReads shouldBe 1
            }
        }

        test("실제 파서로 읽은 붙여넣기는 TextCheck로 간다") {
            runTest(rig.dispatcher) {
                val harness = rig.create(reader = rig.realParserReader())

                harness.pasteNext("学校\t학교\n先生\t선생님")
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.TextCheck
                harness.state.table
                    ?.format shouldBe ImportFormat.TEXT
                harness.state.table
                    ?.sourceName shouldBe ImportViewModelHarness.PASTE_SOURCE_NAME
            }
        }

        test("붙여넣기의 텍스트 확인에서 뒤로 가면 확인 없이 입력을 유지한 채 Paste로 간다") {
            runTest(rig.dispatcher) {
                val table = ImportTestTables.text(sourceName = ImportViewModelHarness.PASTE_SOURCE_NAME)
                val harness = rig.create(reader = FakeImportSourceReader().returns(table))
                harness.pasteNext("a\tb")
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()

                harness.state.dialog shouldBe null
                harness.state.step shouldBe ImportStep.Paste
                harness.state.pasteText shouldBe "a\tb"
            }
        }

        test("Paste 단계에서 뒤로 가면 Source로 가고 화면을 나가지 않는다") {
            runTest(rig.dispatcher) {
                val harness = rig.create()
                harness.send(ImportDeckIntent.ClickPaste)
                advanceUntilIdle()
                harness.state.step shouldBe ImportStep.Paste

                harness.viewModel.sideEffect.test {
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    expectNoEvents()
                }

                harness.state.step shouldBe ImportStep.Source
            }
        }
    }

    private companion object {
        const val AT_LIMIT_TEXT = "1234567890"
        const val OVER_LIMIT_TEXT = "12345678901"
    }
}
