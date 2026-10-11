package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.logic.PreviewBlockReason
import com.whatever.caro.feature.importer.logic.previewBlockReason
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.support.EMPTY_SIDE_TEXT
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.GatedDispatcher
import com.whatever.caro.feature.importer.support.ImportMappingTables
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.openDeckInfo
import com.whatever.caro.feature.importer.support.openMappingFromFile
import com.whatever.caro.feature.importer.support.openMappingFromFixture
import com.whatever.caro.feature.importer.support.openMappingFromPaste
import com.whatever.caro.feature.importer.support.openPreview
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** 미리보기 계산과 취소, 연결을 바꾼 뒤의 재계산, 필터, 장수 상한과 0장 차단, 뒤로 가기를 확인한다. */
class ImportDeckViewModelPreviewTest : FunSpec() {
    private val rig = ImportViewModelRig()

    private class LimitCase(
        val maxCards: Int,
        val rows: Int,
        val rowLimitReached: Boolean,
        val exceeded: Boolean,
    )

    init {
        rig.installOn(this)

        context("미리보기 계산") {
            test("Mapping의 [다음]은 BuildingPreview 오버레이를 띄우고 아직 단계를 옮기지 않는다") {
                runTest(rig.dispatcher) {
                    val gate = GatedDispatcher(rig.dispatcher)
                    val harness = textMapping(gate = gate)
                    gate.hold()

                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    runCurrent()

                    harness.state.loading shouldBe ImportLoading.BuildingPreview
                    harness.state.overlayLoading shouldBe ImportLoading.BuildingPreview
                    harness.state.step shouldBe ImportStep.Mapping
                }
            }

            test("미리보기는 default dispatcher가 일하기 전에는 만들어지지 않는다") {
                runTest(rig.dispatcher) {
                    val gate = GatedDispatcher(rig.dispatcher)
                    val harness = textMapping(gate = gate)
                    gate.hold()

                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    runCurrent()

                    harness.state.loading shouldBe ImportLoading.BuildingPreview
                    harness.state.preview.shouldBeNull()
                }
            }

            test("계산이 끝나면 오버레이를 걷고 Preview로 간다") {
                runTest(rig.dispatcher) {
                    val gate = GatedDispatcher(rig.dispatcher)
                    val harness = textMapping(gate = gate)
                    gate.hold()
                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    runCurrent()

                    gate.release()
                    advanceUntilIdle()

                    harness.state.loading shouldBe null
                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.preview
                        .shouldNotBeNull()
                        .cards.size shouldBe 3
                }
            }

            test("계산 중 뒤로 가기는 계산을 취소하고 Mapping에 머문다") {
                runTest(rig.dispatcher) {
                    val gate = GatedDispatcher(rig.dispatcher)
                    val harness = textMapping(gate = gate)
                    val mappingsBefore = harness.state.mappings
                    gate.hold()
                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    runCurrent()
                    harness.state.loading shouldBe ImportLoading.BuildingPreview

                    harness.send(ImportDeckIntent.ClickBack)
                    runCurrent()
                    gate.release()
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Mapping
                    harness.state.loading shouldBe null
                    harness.state.preview.shouldBeNull()
                    harness.state.dialog shouldBe null
                    harness.state.mappings shouldBe mappingsBefore
                }
            }

            test("빈 면 문구는 [다음] Intent로 받은 값을 쓴다") {
                runTest(rig.dispatcher) {
                    val harness = textMapping()
                    harness.send(ImportDeckIntent.ToggleField(TEXT, FieldMapping.BACK, "열 2"))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickMappingNext("Empty"))
                    advanceUntilIdle()

                    harness.state.preview
                        .shouldNotBeNull()
                        .cards
                        .map { it.back }
                        .toSet() shouldBe setOf("Empty")
                }
            }

            test("연결을 바꾸고 다시 [다음]을 누르면 장수가 새 연결을 따른다") {
                runTest(rig.dispatcher) {
                    val table = ImportMappingTables.twoTypeTable()
                    val harness = rig.create(reader = FakeImportSourceReader().returns(ImportMappingTables.summaryOf(table)).returns(table))
                    openMappingFromFile(harness, "mixed.apkg")
                    openPreview(harness)
                    harness.state.preview
                        .shouldNotBeNull()
                        .stats.cardCount shouldBe 5

                    harness.send(ImportDeckIntent.ClickBack)
                    harness.send(ImportDeckIntent.ToggleNoteType(ImportMappingTables.VOCAB))
                    advanceUntilIdle()
                    openPreview(harness)

                    val preview = harness.state.preview.shouldNotBeNull()
                    preview.stats.cardCount shouldBe 3
                    preview.cards.map { it.front } shouldBe listOf("f0", "f1", "f2")
                }
            }
        }

        context("필터") {
            test("SelectPreviewFilter는 고른 필터를 상태에 둔다") {
                runTest(rig.dispatcher) {
                    val harness = textMapping()
                    openPreview(harness)

                    harness.send(ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.Truncated))
                    advanceUntilIdle()

                    harness.state.previewFilter shouldBe ImportPreviewFilter.Truncated
                }
            }

            test("미리보기를 다시 만들면 필터는 전체로 돌아간다") {
                runTest(rig.dispatcher) {
                    val harness = textMapping()
                    openPreview(harness)
                    harness.send(ImportDeckIntent.SelectPreviewFilter(ImportPreviewFilter.EmptyReplaced))
                    advanceUntilIdle()
                    harness.state.previewFilter shouldBe ImportPreviewFilter.EmptyReplaced
                    harness.send(ImportDeckIntent.ClickBack)
                    harness.send(ImportDeckIntent.ToggleField(TEXT, FieldMapping.BACK, "열 2"))
                    advanceUntilIdle()

                    openPreview(harness)

                    harness.state.previewFilter shouldBe ImportPreviewFilter.All
                }
            }
        }

        context("장수 상한") {
            listOf(
                LimitCase(maxCards = 10_000, rows = 10_000, rowLimitReached = false, exceeded = false),
                LimitCase(maxCards = 10_000, rows = 10_001, rowLimitReached = true, exceeded = true),
                LimitCase(maxCards = 5_000, rows = 5_000, rowLimitReached = false, exceeded = false),
                LimitCase(maxCards = 5_000, rows = 5_001, rowLimitReached = false, exceeded = true),
            ).forEach { case ->
                val outcome = if (case.exceeded) "상한 초과로 [다음]을 막는다" else "[다음]으로 덱 정보에 간다"
                test("maxCards ${case.maxCards}에서 ${case.rows}행을 읽으면 $outcome") {
                    runTest(rig.dispatcher) {
                        val table = ImportTestTables.text(rows = case.rows).copy(rowLimitReached = case.rowLimitReached)
                        val harness = textMapping(table, limits = ImportLimitsConfig(maxCards = case.maxCards))
                        openPreview(harness)

                        openDeckInfo(harness)

                        harness.state.preview
                            .shouldNotBeNull()
                            .limitExceeded shouldBe case.exceeded
                        harness.state.step shouldBe if (case.exceeded) ImportStep.Preview else ImportStep.DeckInfo
                    }
                }
            }

            test("파서가 행 상한에서 멈췄으면 제외된 카드로 장수가 상한 아래여도 상한 초과 표시를 켠다") {
                runTest(rig.dispatcher) {
                    val table = ImportTestTables.text(rows = 100).copy(rowLimitReached = true)
                    val harness = textMapping(table)

                    openPreview(harness)

                    harness.state.preview
                        .shouldNotBeNull()
                        .limitExceeded shouldBe true
                }
            }

            test("텍스트 파일이 10,001줄이면 파일용 상한 이유를 보이고 카드는 계속 보인다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = rig.realParserReader())
                    openMappingFromFixture(harness, "text/large_10001_rows.tsv")
                    openPreview(harness)

                    assertLimitExceeded(harness, PreviewBlockReason.TooManyTextFile)
                }
            }

            test("붙여넣기가 10,001줄이면 붙여넣기용 상한 이유를 보이고 카드는 계속 보인다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = rig.realParserReader())
                    openMappingFromPaste(harness, (1..10_001).joinToString("\n") { "a$it\tb$it" })
                    openPreview(harness)

                    assertLimitExceeded(harness, PreviewBlockReason.TooManyPaste)
                }
            }

            test("엑셀이 10,001행이면 엑셀용 상한 이유를 보이고 카드는 계속 보인다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = rig.realParserReader())
                    openMappingFromFixture(harness, "xlsx/large_10001_rows.xlsx")
                    openPreview(harness)

                    assertLimitExceeded(harness, PreviewBlockReason.TooManyXlsx)
                }
            }
        }

        context("0장") {
            test("연결한 항목이 모두 비어 가져올 카드가 0장이면 이유는 카드 없음이다") {
                runTest(rig.dispatcher) {
                    val harness = textMapping(ImportMappingTables.blankRowsText(rows = 3))

                    openPreview(harness)

                    val preview = harness.state.preview.shouldNotBeNull()
                    preview.cards.shouldBeEmpty()
                    preview.stats.excludedCount shouldBe 3
                    previewBlockReason(
                        preview,
                        harness.state.table
                            .shouldNotBeNull()
                            .format,
                        harness.state.isPaste,
                    ) shouldBe
                        PreviewBlockReason.NoCards
                }
            }

            test("가져올 카드가 0장이면 [다음]을 눌러도 덱 정보로 가지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = textMapping(ImportMappingTables.blankRowsText(rows = 3))
                    openPreview(harness)

                    openDeckInfo(harness)

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.deckName shouldBe ""
                }
            }
        }

        context("뒤로 가기") {
            test("Preview에서 뒤로 가면 확인 없이 Mapping으로 가고 고친 연결을 유지한다") {
                runTest(rig.dispatcher) {
                    val harness = textMapping()
                    harness.send(ImportDeckIntent.ToggleField(TEXT, FieldMapping.BACK, "열 2"))
                    advanceUntilIdle()
                    val edited = harness.state.mappings
                    openPreview(harness)
                    harness.state.step shouldBe ImportStep.Preview

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Mapping
                    harness.state.dialog shouldBe null
                    harness.state.mappings shouldBe edited
                }
            }
        }
    }

    private fun TestScope.textMapping(
        table: ParsedTable = ImportTestTables.text(),
        limits: ImportLimitsConfig = ImportLimitsConfig(),
        gate: GatedDispatcher? = null,
    ): ImportViewModelHarness {
        val reader = FakeImportSourceReader().returns(table)
        val harness =
            if (gate ==
                null
            ) {
                rig.create(reader = reader, limits = limits)
            } else {
                rig.create(reader = reader, limits = limits, defaultDispatcher = gate)
            }
        openMappingFromFile(harness, "words.txt")
        return harness
    }

    private fun assertLimitExceeded(
        harness: ImportViewModelHarness,
        expected: PreviewBlockReason,
    ) {
        val preview = harness.state.preview.shouldNotBeNull()
        preview.limitExceeded shouldBe true
        preview.cards.isNotEmpty() shouldBe true
        previewBlockReason(
            preview,
            harness.state.table
                .shouldNotBeNull()
                .format,
            harness.state.isPaste,
        ) shouldBe expected
    }

    private companion object {
        const val TEXT = "text"
    }
}
