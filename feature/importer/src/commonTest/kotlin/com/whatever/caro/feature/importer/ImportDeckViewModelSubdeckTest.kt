package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
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
import kotlinx.coroutines.test.runTest

/** Anki 하위 덱 선택 단계: 요약 읽기, 선택 토글, 고른 id로 다시 읽기, 장수 상한을 확인한다. */
class ImportDeckViewModelSubdeckTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val modernSummary by lazy { ImportTestTables.apkgSummary("modern_anki21b_main.apkg") }
    private val edgeSummary by lazy { ImportTestTables.apkgSummary("legacy_edge_cases.apkg") }
    private val largeSummary by lazy { ImportTestTables.apkgSummary("large_10001_notes_modern.apkg") }

    private class LimitCase(
        val maxCards: Int,
        val deselected: List<String>,
        val selectedTotal: Int,
        val blocked: Boolean,
    )

    init {
        rig.installOn(this)

        test("apkg 첫 읽기는 subdeckIds를 빈 집합으로 해 요약만 읽는다") {
            runTest(rig.dispatcher) {
                val harness = pickedApkg(FakeImportSourceReader().returns(modernSummary))

                val call =
                    harness.reader.calls
                        .single()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                call.subdeckIds shouldBe emptySet<String>()
                call.sheetIndex shouldBe 0
                call.delimiter shouldBe null
                call.hasHeaderRow shouldBe null
            }
        }

        test("apkg 요약을 읽으면 SubdeckSelect로 가서 모든 하위 덱을 고른다") {
            runTest(rig.dispatcher) {
                val harness = pickedApkg(FakeImportSourceReader().returns(modernSummary))

                harness.state.step shouldBe ImportStep.SubdeckSelect
                harness.state.loading shouldBe null
                harness.state.table shouldBe modernSummary
                harness.state.selection.subdeckIds
                    .toSet() shouldBe modernSummary.subdecks.map { subdeck -> subdeck.id }.toSet()
            }
        }

        test("카드가 없는 노트 묶음인 덱 없음은 기본으로 고르지 않는다") {
            runTest(rig.dispatcher) {
                val harness = pickedApkg(FakeImportSourceReader().returns(edgeSummary))

                harness.state.selection.subdeckIds
                    .toSet() shouldBe setOf(EDGE_SUBDECK)
            }
        }

        test("ToggleSubdeck은 고른 하위 덱을 풀고 다시 누르면 고른다") {
            runTest(rig.dispatcher) {
                val harness = pickedApkg(FakeImportSourceReader().returns(modernSummary))

                harness.send(ImportDeckIntent.ToggleSubdeck(GRAMMAR))
                advanceUntilIdle()
                harness.state.selection.subdeckIds
                    .toSet() shouldBe setOf(VOCAB_N4, VOCAB_N5)

                harness.send(ImportDeckIntent.ToggleSubdeck(GRAMMAR))
                advanceUntilIdle()
                harness.state.selection.subdeckIds
                    .toSet() shouldBe setOf(GRAMMAR, VOCAB_N4, VOCAB_N5)
            }
        }

        test("ToggleAllSubdecks는 전체가 선택된 상태에서 모두 풀고 아무것도 없으면 모두 고른다") {
            runTest(rig.dispatcher) {
                val harness = pickedApkg(FakeImportSourceReader().returns(modernSummary))

                harness.send(ImportDeckIntent.ToggleAllSubdecks)
                advanceUntilIdle()
                harness.state.selection.subdeckIds
                    .toSet() shouldBe emptySet<String>()

                harness.send(ImportDeckIntent.ToggleAllSubdecks)
                advanceUntilIdle()
                harness.state.selection.subdeckIds
                    .toSet() shouldBe setOf(GRAMMAR, VOCAB_N4, VOCAB_N5)
            }
        }

        test("ToggleAllSubdecks는 덱 없음 선택을 건드리지 않는다") {
            runTest(rig.dispatcher) {
                val harness = pickedApkg(FakeImportSourceReader().returns(edgeSummary))
                harness.send(ImportDeckIntent.ToggleSubdeck(ParsedSubdeck.NO_DECK_ID))
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ToggleAllSubdecks)
                advanceUntilIdle()

                harness.state.selection.subdeckIds
                    .toSet() shouldBe setOf(ParsedSubdeck.NO_DECK_ID)
            }
        }

        test("[다음]은 고른 하위 덱 id로 같은 사본을 다시 읽고 Reparsing 오버레이를 보여준다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(modernSummary)
                reader.suspends()
                val harness = pickedApkg(reader)
                harness.send(ImportDeckIntent.ToggleSubdeck(GRAMMAR))
                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                val first =
                    harness.reader.calls
                        .first()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                val second =
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                second.subdeckIds shouldBe setOf(VOCAB_N4, VOCAB_N5)
                second.path shouldBe first.path
                harness.state.loading shouldBe ImportLoading.Reparsing("deck.apkg")
                harness.state.step shouldBe ImportStep.SubdeckSelect
            }
        }

        test("덱 없음을 고르면 다시 읽을 때 NO_DECK_ID도 함께 넘긴다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(edgeSummary).returns(ImportTestTables.withOneRow(edgeSummary))
                val harness = pickedApkg(reader)
                harness.send(ImportDeckIntent.ToggleSubdeck(ParsedSubdeck.NO_DECK_ID))
                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                val second =
                    harness.reader.calls
                        .last()
                        .shouldBeInstanceOf<ReadCall.FileRead>()
                second.subdeckIds shouldBe setOf(EDGE_SUBDECK, ParsedSubdeck.NO_DECK_ID)
            }
        }

        test("다시 읽기가 끝나면 Image Occlusion을 뺀 기본 연결로 Mapping에 간다") {
            runTest(rig.dispatcher) {
                val rows = ImportTestTables.apkg("modern_image_occlusion.apkg", "default")
                val reader = FakeImportSourceReader().returns(ImportTestTables.apkgSummary("modern_image_occlusion.apkg")).returns(rows)
                val harness = pickedApkg(reader)

                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.Mapping
                harness.state.loading shouldBe null
                harness.state.table shouldBe rows
                harness.state.mappings.keys shouldBe setOf(BASIC_TYPE)
                harness.state.mappings[BASIC_TYPE] shouldBe
                    FieldMapping(
                        noteTypeId = BASIC_TYPE,
                        targets = mapOf(FieldMapping.FRONT to listOf("Front"), FieldMapping.BACK to listOf("Back")),
                    )
            }
        }

        test("첫 읽기에서 받은 dateStyle을 다시 읽기에도 그대로 쓴다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(modernSummary).returns(ImportTestTables.withOneRow(modernSummary))
                val harness = pickedApkg(reader, dateStyle = SpreadsheetDateStyle.MONTH_DAY_YEAR)

                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                harness.reader.calls
                    .map { call -> call.dateStyle }
                    .toSet() shouldBe setOf(SpreadsheetDateStyle.MONTH_DAY_YEAR)
            }
        }

        test("하위 덱을 다시 읽는 중 뒤로 가기는 읽기를 취소하고 선택을 유지한 채 SubdeckSelect에 머문다") {
            runTest(rig.dispatcher) {
                val reader = FakeImportSourceReader().returns(modernSummary)
                val gate = reader.suspends()
                val harness = pickedApkg(reader)
                harness.send(ImportDeckIntent.ToggleSubdeck(GRAMMAR))
                advanceUntilIdle()
                val selectionBefore = harness.state.selection
                harness.send(ImportDeckIntent.ClickSelectionNext)
                advanceUntilIdle()

                harness.send(ImportDeckIntent.ClickBack)
                advanceUntilIdle()
                gate.complete(ImportTestTables.withOneRow(modernSummary))
                advanceUntilIdle()

                harness.state.step shouldBe ImportStep.SubdeckSelect
                harness.state.loading shouldBe null
                harness.state.dialog shouldBe null
                harness.state.selection shouldBe selectionBefore
                harness.state.table shouldBe modernSummary
                harness.reader.cancelledReads shouldBe 1
            }
        }

        listOf(
            LimitCase(maxCards = 10_000, deselected = emptyList(), selectedTotal = 10_001, blocked = true),
            LimitCase(maxCards = 10_000, deselected = listOf(BIG_PART3), selectedTotal = 10_000, blocked = false),
            LimitCase(maxCards = 5_000, deselected = listOf(BIG_PART3), selectedTotal = 10_000, blocked = true),
            LimitCase(maxCards = 5_000, deselected = listOf(BIG_PART2), selectedTotal = 5_001, blocked = true),
            LimitCase(maxCards = 5_000, deselected = listOf(BIG_PART2, BIG_PART3), selectedTotal = 5_000, blocked = false),
        ).forEach { case ->
            val outcome = if (case.blocked) "막는다" else "통과시킨다"
            test("maxCards ${case.maxCards}에서 고른 하위 덱 합계가 ${case.selectedTotal}장이면 [다음]을 $outcome") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(largeSummary)
                    if (!case.blocked) reader.returns(ImportTestTables.withOneRow(largeSummary))
                    val harness = pickedApkg(reader, limits = ImportLimitsConfig(maxCards = case.maxCards))
                    case.deselected.forEach { id -> harness.send(ImportDeckIntent.ToggleSubdeck(id)) }

                    harness.send(ImportDeckIntent.ClickSelectionNext)
                    advanceUntilIdle()

                    harness.reader.calls.size shouldBe if (case.blocked) 1 else 2
                    if (case.blocked) {
                        harness.state.step shouldBe ImportStep.SubdeckSelect
                        harness.state.loading shouldBe null
                    }
                }
            }
        }
    }

    private fun TestScope.pickedApkg(
        reader: FakeImportSourceReader,
        limits: ImportLimitsConfig = ImportLimitsConfig(),
        dateStyle: SpreadsheetDateStyle = SpreadsheetDateStyle.YEAR_MONTH_DAY,
    ): ImportViewModelHarness {
        val harness = rig.create(reader = reader, limits = limits)
        harness.pick(FakePickedImportFile.ofSize("deck.apkg", size = 100), dateStyle)
        advanceUntilIdle()
        return harness
    }

    private companion object {
        const val GRAMMAR = "1791602016834"
        const val VOCAB_N4 = "1791602016837"
        const val VOCAB_N5 = "1791602016836"
        const val EDGE_SUBDECK = "2059400010"
        const val BASIC_TYPE = "1791602016919"
        const val BIG_PART2 = "1791602017098"
        const val BIG_PART3 = "1791602017099"
    }
}
