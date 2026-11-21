package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.feature.importer.logic.NoCardsKind
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
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 붙여넣기: 길이 상한과 오류 상태, 첫 읽기와 판정, 읽기 실패를 확인한다. */
class ImportDeckViewModelPasteTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val shortLimit = ImportLimitsConfig(maxPasteChars = 10)
    private val pasteTable by lazy { ImportTestTables.text(sourceName = ImportViewModelHarness.PASTE_SOURCE_NAME) }

    init {
        rig.installOn(this)

        context("길이 상한") {
            test("상한을 한 글자 넘는 글은 자르지 않고 보관하고 오류 상태로 [다음]을 끈다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(limits = shortLimit)

                    harness.pasteNext(OVER_LIMIT_TEXT)
                    advanceUntilIdle()

                    harness.state.pasteText shouldBe OVER_LIMIT_TEXT
                    harness.state.isPasteOverflowed shouldBe true
                    harness.state.isPasteNextEnabled shouldBe false
                    harness.state.step shouldBe ImportStep.Paste
                    harness.reader.calls.shouldBeEmpty()
                }
            }

            test("상한을 크게 넘는 글은 상한 + 1자까지만 보관하고 읽지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(limits = shortLimit)

                    harness.pasteNext("x".repeat(1_000))
                    advanceUntilIdle()

                    harness.state.pasteText shouldBe "x".repeat(11)
                    harness.reader.calls.shouldBeEmpty()
                }
            }

            test("상한 바로 뒤가 서로게이트 쌍 가운데여도 보관한 글은 상한을 넘어 읽지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(limits = shortLimit)

                    harness.pasteNext(AT_LIMIT_TEXT + "😀😀")
                    advanceUntilIdle()

                    harness.state.pasteText.length shouldBe 11
                    harness.reader.calls.shouldBeEmpty()
                }
            }

            test("넘친 붙여넣기에서 글자를 지워 상한 이하로 돌아와도 오류 상태가 남는다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(limits = shortLimit)
                    harness.send(ImportDeckIntent.ClickPaste)
                    harness.send(ImportDeckIntent.UpdatePasteText(OVER_LIMIT_TEXT))

                    harness.send(ImportDeckIntent.UpdatePasteText(AT_LIMIT_TEXT))
                    harness.send(clickNext())
                    advanceUntilIdle()

                    harness.state.pasteText shouldBe AT_LIMIT_TEXT
                    harness.state.isPasteOverflowed shouldBe true
                    harness.reader.calls.shouldBeEmpty()
                }
            }

            test("넘친 붙여넣기의 가운데를 다른 글자로 바꿔 상한 이하로 돌아와도 오류 상태가 남아 읽지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(pasteTable), limits = shortLimit)
                    harness.send(ImportDeckIntent.ClickPaste)
                    harness.send(ImportDeckIntent.UpdatePasteText(OVER_LIMIT_TEXT))

                    harness.send(ImportDeckIntent.UpdatePasteText("123X8901"))
                    harness.send(clickNext())
                    advanceUntilIdle()

                    harness.state.isPasteOverflowed shouldBe true
                    harness.state.isPasteNextEnabled shouldBe false
                    harness.reader.calls.shouldBeEmpty()
                }
            }

            test("넘친 붙여넣기를 비우고 새로 붙여 넣으면 오류 상태가 풀리고 읽는다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(pasteTable), limits = shortLimit)
                    harness.send(ImportDeckIntent.ClickPaste)
                    harness.send(ImportDeckIntent.UpdatePasteText(OVER_LIMIT_TEXT))
                    harness.send(ImportDeckIntent.UpdatePasteText(""))

                    harness.send(ImportDeckIntent.UpdatePasteText("a\tb"))
                    harness.send(clickNext())
                    advanceUntilIdle()

                    harness.reader.calls.size shouldBe 1
                }
            }

            test("비어 있는 붙여넣기는 [다음]이 꺼져 읽지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create()
                    harness.send(ImportDeckIntent.ClickPaste)

                    harness.send(clickNext())
                    advanceUntilIdle()

                    harness.state.isPasteNextEnabled shouldBe false
                    harness.state.isPasteOverflowed shouldBe false
                    harness.reader.calls.shouldBeEmpty()
                    harness.state.loading shouldBe null
                }
            }

            test("상한 길이 그대로의 붙여넣기는 읽는다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(pasteTable), limits = shortLimit)

                    harness.pasteNext(AT_LIMIT_TEXT)
                    advanceUntilIdle()

                    harness.reader.calls.size shouldBe 1
                }
            }

            test("Paste가 아닌 단계의 입력은 버린다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create()

                    harness.send(ImportDeckIntent.UpdatePasteText("a\tb"))
                    harness.send(clickNext())
                    advanceUntilIdle()

                    harness.state.pasteText shouldBe ""
                    harness.reader.calls.shouldBeEmpty()
                }
            }
        }

        context("읽기와 판정") {
            test("[다음]은 넘겨받은 sourceName과 dateStyle로 글을 읽고 파일 이름 없는 덮개를 보인다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader()
                    reader.suspends()
                    val harness = rig.create(reader = reader)
                    harness.send(ImportDeckIntent.ClickPaste)
                    harness.send(ImportDeckIntent.UpdatePasteText("a\tb"))

                    harness.send(
                        ImportDeckIntent.ClickPasteNext(
                            "Pasted cards",
                            SpreadsheetDateStyle.MONTH_DAY_YEAR,
                            "(empty)",
                            "Created from pasted text",
                        ),
                    )
                    advanceUntilIdle()

                    val call =
                        harness.reader.calls
                            .single()
                            .shouldBeInstanceOf<ReadCall.TextRead>()
                    call.text shouldBe "a\tb"
                    call.sourceName shouldBe "Pasted cards"
                    call.dateStyle shouldBe SpreadsheetDateStyle.MONTH_DAY_YEAR
                    harness.state.loading shouldBe ImportLoading.ReadingPaste
                }
            }

            test("[다음]을 연달아 눌러도 한 번만 읽는다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(pasteTable))
                    harness.send(ImportDeckIntent.ClickPaste)
                    harness.send(ImportDeckIntent.UpdatePasteText("a\tb"))

                    harness.send(clickNext())
                    harness.send(clickNext())
                    advanceUntilIdle()

                    harness.reader.calls.size shouldBe 1
                    harness.state.step shouldBe ImportStep.Preview
                }
            }

            test("실제 파서로 읽은 붙여넣기는 미리보기로 간다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = rig.realParserReader())

                    harness.pasteNext("学校\t학교\n先生\t선생님")
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.table
                        ?.sourceName shouldBe ImportViewModelHarness.PASTE_SOURCE_NAME
                    harness.state.preview
                        .shouldNotBeNull()
                        .cards
                        .map { card -> card.back } shouldBe listOf("학교", "선생님")
                }
            }

            test("카드로 만들 내용이 없으면 카드 없음 대화상자를 띄우고 [확인]을 누르면 입력을 남긴 채 Paste에 머문다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.blankText()))
                    harness.pasteNext("\n\n")
                    advanceUntilIdle()
                    harness.state.dialog shouldBe ImportDialog.NoCards(NoCardsKind.NoContent)
                    harness.state.step shouldBe ImportStep.Paste

                    harness.send(ImportDeckIntent.DismissDialog)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe ImportStep.Paste
                    harness.state.isPaste shouldBe true
                    harness.state.pasteText shouldBe "\n\n"
                }
            }

            test("붙여넣기가 상한을 넘으면 너무 많다는 대화상자를 띄우고 뒤로 가기는 [확인]과 같이 입력을 남긴 채 Paste에 머문다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ImportTestTables.text(rows = 4))
                    val harness = rig.create(reader = reader, limits = ImportLimitsConfig(maxCards = 3))
                    harness.pasteNext("a\tb")
                    advanceUntilIdle()
                    harness.state.dialog shouldBe ImportDialog.TooManyCards(isPaste = true)
                    harness.state.step shouldBe ImportStep.Paste

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe ImportStep.Paste
                    harness.state.pasteText shouldBe "a\tb"
                    harness.state.table shouldBe null
                }
            }

            test("붙여넣기 읽기가 실패하면 붙여넣기 오류 대화상자를 띄우고 입력을 유지한 채 Paste에 머문다") {
                runTest(rig.dispatcher) {
                    val error = ImportParseException(ImportParseErrorKind.TOO_MANY_COLUMNS, "forced")
                    val harness = rig.create(reader = FakeImportSourceReader().fails(error))

                    harness.pasteNext("a\tb")
                    advanceUntilIdle()

                    harness.state.dialog shouldBe
                        ImportDialog.ParseError(ImportParseErrorKind.TOO_MANY_COLUMNS, extension = "", isPaste = true)
                    harness.state.step shouldBe ImportStep.Paste
                    harness.state.pasteText shouldBe "a\tb"
                    harness.state.loading shouldBe null
                }
            }
        }
    }

    private fun clickNext() =
        ImportDeckIntent.ClickPasteNext(
            ImportViewModelHarness.PASTE_SOURCE_NAME,
            ImportViewModelHarness.DATE_STYLE,
            ImportViewModelHarness.EMPTY_SIDE_TEXT,
            ImportViewModelHarness.PASTE_DESCRIPTION,
        )

    private companion object {
        const val AT_LIMIT_TEXT = "1234567890"
        const val OVER_LIMIT_TEXT = "12345678901"
    }
}
