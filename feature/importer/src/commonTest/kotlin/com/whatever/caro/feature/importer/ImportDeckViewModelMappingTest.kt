package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.logic.MappingBlockReason
import com.whatever.caro.feature.importer.logic.mappingBlockReason
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.EMPTY_SIDE_TEXT
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.openMappingFromFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 항목 연결 단계: 유형 켜고 끄기, 항목 고르기, 미리보기를 막는 이유를 확인한다. */
class ImportDeckViewModelMappingTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val summary by lazy { ImportTestTables.apkgSummary("modern_anki21b_main.apkg") }
    private val allSubdecks by lazy { ImportTestTables.apkg("modern_anki21b_main.apkg", "default") }
    private val occlusionSummary by lazy { ImportTestTables.apkgSummary("modern_image_occlusion.apkg") }
    private val occlusionTable by lazy { ImportTestTables.apkg("modern_image_occlusion.apkg", "default") }

    init {
        rig.installOn(this)

        context("항목 고르기") {
            test("ToggleField는 이미 고른 항목을 누르면 선택을 풀어 뒤 항목을 당긴다") {
                runTest(rig.dispatcher) {
                    val harness = apkgMapping()

                    harness.send(ImportDeckIntent.ToggleField(VOCAB, FieldMapping.BACK, "Meaning"))
                    advanceUntilIdle()

                    harness.state.mappings
                        .getValue(VOCAB)
                        .backFields shouldBe listOf("Example", "Reading")
                }
            }

            test("ToggleField로 푼 항목을 다시 고르면 뒷면 맨 뒤에 놓는다") {
                runTest(rig.dispatcher) {
                    val harness = apkgMapping()
                    harness.send(ImportDeckIntent.ToggleField(VOCAB, FieldMapping.BACK, "Meaning"))

                    harness.send(ImportDeckIntent.ToggleField(VOCAB, FieldMapping.BACK, "Meaning"))
                    advanceUntilIdle()

                    harness.state.mappings
                        .getValue(VOCAB)
                        .backFields shouldBe listOf("Example", "Reading", "Meaning")
                }
            }

            test("ToggleField는 다른 면에 놓인 항목을 고르면 그 면 맨 뒤로 옮긴다") {
                runTest(rig.dispatcher) {
                    val harness = apkgMapping()

                    harness.send(ImportDeckIntent.ToggleField(VOCAB, FieldMapping.FRONT, "Reading"))
                    advanceUntilIdle()

                    val vocab = harness.state.mappings.getValue(VOCAB)
                    vocab.frontFields shouldBe listOf("Word", "Reading")
                    vocab.backFields shouldBe listOf("Meaning", "Example")
                }
            }

            test("ToggleField는 고른 유형 하나의 연결만 바꾼다") {
                runTest(rig.dispatcher) {
                    val harness = apkgMapping()
                    val basicBefore = harness.state.mappings.getValue(BASIC)

                    harness.send(ImportDeckIntent.ToggleField(VOCAB, FieldMapping.BACK, "Example"))
                    advanceUntilIdle()

                    harness.state.mappings
                        .getValue(VOCAB)
                        .backFields shouldBe listOf("Meaning", "Reading")
                    harness.state.mappings
                        .getValue(BASIC) shouldBe basicBefore
                }
            }

            test("ToggleNoteType은 가져오기를 끄고 항목 연결은 그대로 둔다") {
                runTest(rig.dispatcher) {
                    val harness = apkgMapping()
                    val vocabBefore = harness.state.mappings.getValue(VOCAB)

                    harness.send(ImportDeckIntent.ToggleNoteType(VOCAB))
                    advanceUntilIdle()

                    harness.state.mappings
                        .getValue(VOCAB) shouldBe vocabBefore.copy(excluded = true)
                }
            }

            test("ToggleNoteType을 다시 누르면 가져오기를 켠다") {
                runTest(rig.dispatcher) {
                    val harness = apkgMapping()
                    val vocabBefore = harness.state.mappings.getValue(VOCAB)
                    harness.send(ImportDeckIntent.ToggleNoteType(VOCAB))
                    advanceUntilIdle()
                    harness.state.mappings
                        .getValue(VOCAB)
                        .excluded shouldBe true

                    harness.send(ImportDeckIntent.ToggleNoteType(VOCAB))
                    advanceUntilIdle()

                    harness.state.mappings
                        .getValue(VOCAB) shouldBe vocabBefore
                }
            }

            test("Image Occlusion 유형의 켜고 끄기와 항목 고르기는 무시한다") {
                runTest(rig.dispatcher) {
                    val harness = occlusionMapping()
                    harness.send(ImportDeckIntent.ToggleNoteType(OCCLUSION_BASIC))

                    harness.send(ImportDeckIntent.ToggleNoteType(OCCLUSION))
                    harness.send(ImportDeckIntent.ToggleField(OCCLUSION, FieldMapping.FRONT, "Image"))
                    advanceUntilIdle()

                    harness.state.mappings
                        .getValue(OCCLUSION_BASIC)
                        .excluded shouldBe true
                    harness.state.mappings.keys shouldBe setOf(OCCLUSION_BASIC)
                }
            }
        }

        context("미리보기를 막는 이유") {
            test("모든 유형을 끄면 이유는 켠 유형이 없음이고 [다음]을 눌러도 미리보기를 만들지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = apkgMapping()
                    listOf(BASIC, REVERSED, VOCAB, CLOZE).forEach { id -> harness.send(ImportDeckIntent.ToggleNoteType(id)) }
                    advanceUntilIdle()

                    mappingBlockReason(harness.state.table.shouldNotBeNull(), harness.state.mappings) shouldBe
                        MappingBlockReason.NoTypeEnabled
                    assertStaysOnMapping(harness)
                }
            }

            test("켠 유형의 앞면과 뒷면 항목을 모두 풀면 이유는 항목 없음이고 [다음]을 막는다") {
                runTest(rig.dispatcher) {
                    val harness = textMapping()
                    harness.send(ImportDeckIntent.ToggleField(TEXT, FieldMapping.FRONT, "열 1"))
                    harness.send(ImportDeckIntent.ToggleField(TEXT, FieldMapping.BACK, "열 2"))
                    advanceUntilIdle()

                    mappingBlockReason(harness.state.table.shouldNotBeNull(), harness.state.mappings) shouldBe
                        MappingBlockReason.NoFieldSelected
                    assertStaysOnMapping(harness)
                }
            }

            test("고른 범위의 유형이 Image Occlusion뿐이면 이유는 가져올 유형 없음이고 [다음]을 막는다") {
                runTest(rig.dispatcher) {
                    val occlusionOnly = occlusionTable.copy(noteTypes = occlusionTable.noteTypes.map { type -> type.occlusionOnly() })
                    val harness = mappingOf(FakeImportSourceReader().returns(occlusionSummary).returns(occlusionOnly), "deck.apkg")

                    mappingBlockReason(harness.state.table.shouldNotBeNull(), harness.state.mappings) shouldBe
                        MappingBlockReason.NoImportableType
                    assertStaysOnMapping(harness)
                }
            }

            test("뒷면 항목만 비어 있으면 막지 않고 그 카드의 뒷면에 빈 면 문구를 넣는다") {
                runTest(rig.dispatcher) {
                    val harness = textMapping()
                    harness.send(ImportDeckIntent.ToggleField(TEXT, FieldMapping.BACK, "열 2"))
                    advanceUntilIdle()

                    harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Preview
                    val cards =
                        harness.state.preview
                            .shouldNotBeNull()
                            .cards
                    cards.size shouldBe 3
                    cards.map { it.back }.toSet() shouldBe setOf(EMPTY_SIDE_TEXT)
                    cards.all { it.backReplaced && !it.frontReplaced } shouldBe true
                }
            }
        }
    }

    private fun ParsedNoteType.occlusionOnly() = if (isImageOcclusion) this else copy(selectedNoteCount = 0)

    private fun TestScope.mappingOf(
        reader: FakeImportSourceReader,
        fileName: String,
    ): ImportViewModelHarness {
        val harness = rig.create(reader = reader)
        openMappingFromFile(harness, fileName)
        return harness
    }

    private fun TestScope.apkgMapping(): ImportViewModelHarness =
        mappingOf(FakeImportSourceReader().returns(summary).returns(allSubdecks), "deck.apkg")

    private fun TestScope.occlusionMapping(): ImportViewModelHarness =
        mappingOf(FakeImportSourceReader().returns(occlusionSummary).returns(occlusionTable), "deck.apkg")

    private fun TestScope.textMapping(): ImportViewModelHarness =
        mappingOf(FakeImportSourceReader().returns(ImportTestTables.text()), "words.txt")

    /** [다음]을 눌러도 BuildingPreview로 가지 않고 Mapping에 머문다. */
    private fun TestScope.assertStaysOnMapping(harness: ImportViewModelHarness) {
        harness.state.step shouldBe ImportStep.Mapping
        harness.send(ImportDeckIntent.ClickMappingNext(EMPTY_SIDE_TEXT))
        advanceUntilIdle()
        harness.state.step shouldBe ImportStep.Mapping
        harness.state.loading shouldBe null
        harness.state.preview.shouldBeNull()
    }

    private companion object {
        const val BASIC = "1700000001"
        const val REVERSED = "1700000002"
        const val VOCAB = "1700000003"
        const val CLOZE = "1700000004"
        const val TEXT = "text"
        const val OCCLUSION = "1791602016924"
        const val OCCLUSION_BASIC = "1791602016919"
    }
}
