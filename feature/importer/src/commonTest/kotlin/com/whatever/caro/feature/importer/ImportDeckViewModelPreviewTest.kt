package com.whatever.caro.feature.importer

import app.cash.turbine.test
import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 미리보기: 필터 칩, 카드 전체 보기, 설정 확인 띠, 뒤로 가기와 처음으로 돌아가기를 확인한다. */
class ImportDeckViewModelPreviewTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        context("필터 칩") {
            test("미리보기를 열면 전체 칩을 고른 상태로 칩마다의 카드 수를 보인다") {
                runTest(rig.dispatcher) {
                    val harness = preview(changedCards())

                    harness.state.previewFilter shouldBe ImportPreviewFilter.All
                    harness.state.filterCounts shouldBe
                        mapOf(
                            ImportPreviewFilter.All to 3,
                            ImportPreviewFilter.EmptyReplaced to 1,
                            ImportPreviewFilter.Truncated to 1,
                            ImportPreviewFilter.MediaRemoved to 0,
                        )
                }
            }

            test("칩을 고르면 그 칩에 해당하는 카드만 거른 목록을 보이고 전체를 고르면 모두 보인다") {
                runTest(rig.dispatcher) {
                    val harness = preview(changedCards())

                    harness.send(ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.Truncated))
                    advanceUntilIdle()
                    harness.state.previewFilter shouldBe ImportPreviewFilter.Truncated
                    harness.state.filteredCards.map { card -> card.sourceIndex } shouldBe listOf(1)

                    harness.send(ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.All))
                    advanceUntilIdle()
                    harness.state.filteredCards shouldBe harness.state.preview?.cards
                }
            }
        }

        context("카드 전체 보기") {
            test("카드를 누르면 그 카드의 전체 보기를 열고 닫기를 누르면 닫는다") {
                runTest(rig.dispatcher) {
                    val harness = preview(ImportTestTables.text())
                    val card = harness.state.filteredCards[1]

                    harness.send(ImportDeckIntent.ClickCard(card))
                    advanceUntilIdle()
                    harness.state.detailCard shouldBe card
                    harness.state.step shouldBe ImportStep.Preview

                    harness.send(ImportDeckIntent.CloseCardDetail)
                    advanceUntilIdle()
                    harness.state.detailCard shouldBe null
                }
            }

            test("전체 보기가 열린 동안 뒤로 가기는 전체 보기만 닫는다") {
                runTest(rig.dispatcher) {
                    val harness = preview(ImportTestTables.text())
                    harness.send(ImportDeckIntent.ClickCard(harness.state.filteredCards.first()))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.detailCard shouldBe null
                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe ImportStep.Preview
                }
            }

            test("전체 보기가 열린 동안 칩과 다른 카드 누름은 버린다") {
                runTest(rig.dispatcher) {
                    val harness = preview(changedCards())
                    val first = harness.state.filteredCards.first()
                    harness.send(ImportDeckIntent.ClickCard(first))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickCard(harness.state.filteredCards.last()))
                    harness.send(ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.Truncated))
                    advanceUntilIdle()

                    harness.state.detailCard shouldBe first
                    harness.state.previewFilter shouldBe ImportPreviewFilter.All
                }
            }
        }

        context("설정 확인 띠") {
            test("절반 이상의 줄이 한 칸이면 칸으로 나뉘지 않은 줄 수와 함께 띠를 보인다") {
                runTest(rig.dispatcher) {
                    val harness = preview(singleFieldText())

                    harness.state.delimiterBannerRowCount shouldBe 3
                }
            }

            test("구분 기준 확인이 필요 없으면 띠가 없다") {
                runTest(rig.dispatcher) {
                    preview(ImportTestTables.text()).state.delimiterBannerRowCount shouldBe null
                }
            }

            test("띠를 닫으면 다시 보이지 않고 새 원본을 읽으면 다시 보인다") {
                runTest(rig.dispatcher) {
                    val harness = preview(singleFieldText(), FakeImportSourceReader().returns(singleFieldText()).returns(singleFieldText()))

                    harness.send(ImportDeckIntent.DismissDelimiterBanner)
                    advanceUntilIdle()
                    harness.state.isDelimiterBannerDismissed shouldBe true
                    harness.state.delimiterBannerRowCount shouldBe null

                    harness.send(ImportDeckIntent.ClickBack)
                    harness.send(ImportDeckIntent.ConfirmDiscard)
                    harness.pickNamed("words.txt")
                    advanceUntilIdle()
                    harness.state.delimiterBannerRowCount shouldBe 3
                }
            }
        }

        context("뒤로 가기") {
            test("[돌아가기]를 누르면 읽은 내용과 임시 파일을 비우고 Source로 간다") {
                runTest(rig.dispatcher) {
                    val harness = preview(ImportTestTables.text())
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ConfirmDiscard)
                    advanceUntilIdle()

                    harness.state shouldBe ImportDeckState()
                    harness.tempStorage.regularFiles().shouldBeEmpty()
                }
            }

            test("[취소]와 뒤로 가기는 묻는 대화상자만 닫고 미리보기를 그대로 둔다") {
                runTest(rig.dispatcher) {
                    val harness = preview(ImportTestTables.text())
                    val before = harness.state

                    listOf(ImportDeckIntent.DismissDialog, ImportDeckIntent.ClickBack).forEach { close ->
                        harness.send(ImportDeckIntent.ClickBack)
                        advanceUntilIdle()
                        harness.viewModel.sideEffect.test {
                            harness.send(close)
                            advanceUntilIdle()
                            expectNoEvents()
                        }

                        harness.state shouldBe before
                    }
                }
            }

            test("묻는 대화상자가 떠 있는 동안 칩, 카드, 띠 닫기 입력은 버린다") {
                runTest(rig.dispatcher) {
                    val harness = preview(singleFieldText())
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    val before = harness.state

                    harness.send(ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.EmptyReplaced))
                    harness.send(ImportDeckIntent.ClickCard(before.filteredCards.first()))
                    harness.send(ImportDeckIntent.DismissDelimiterBanner)
                    advanceUntilIdle()

                    harness.state shouldBe before
                }
            }

            test("붙여넣기 미리보기에서 뒤로 가면 묻지 않고 입력을 남긴 채 Paste로 간다") {
                runTest(rig.dispatcher) {
                    val table = ImportTestTables.text(sourceName = ImportViewModelHarness.PASTE_SOURCE_NAME)
                    val harness = rig.create(reader = FakeImportSourceReader().returns(table))
                    harness.pasteNext("a\tb")
                    advanceUntilIdle()
                    harness.state.step shouldBe ImportStep.Preview

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe ImportStep.Paste
                    harness.state.pasteText shouldBe "a\tb"
                    harness.state.table shouldBe null
                    harness.state.preview shouldBe null
                }
            }
        }
    }

    private fun TestScope.preview(
        table: ParsedTable,
        reader: FakeImportSourceReader = FakeImportSourceReader().returns(table),
    ): ImportViewModelHarness {
        val harness = rig.create(reader = reader)
        harness.pickNamed("words.txt")
        advanceUntilIdle()
        harness.state.step shouldBe ImportStep.Preview
        return harness
    }

    /** 뒷면이 빈 카드, 앞면이 500자를 넘는 카드, 바뀐 것 없는 카드를 차례로 만든다. */
    private fun changedCards(): ParsedTable {
        val base = ImportTestTables.text(rows = 3)
        val names = base.noteTypes.single().fieldNames
        val values = listOf(listOf("앞", ""), listOf("x".repeat(600), "뒤"), listOf("a", "b"))
        return base.copy(rows = base.rows.zip(values) { row, fields -> row.copy(fields = names.zip(fields).toMap()) })
    }

    private fun singleFieldText(): ParsedTable =
        ImportTestTables.text(rows = 3).copy(layout = ParsedLayout(delimiter = "\t", hasHeaderRow = false, singleFieldRowCount = 3))
}
