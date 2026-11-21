package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.logic.NoCardsKind
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.ReadCall
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8

/** 파일 첫 읽기 뒤 판정: 형식별로 미리보기에 가는 길과, 카드가 없거나 너무 많을 때의 대화상자를 확인한다. */
class ImportDeckViewModelFirstReadTest : FunSpec() {
    private val rig = ImportViewModelRig()
    private val ankiSummary by lazy { ImportTestTables.apkgSummary("modern_anki21b_main.apkg") }
    private val ankiFull by lazy { ImportTestTables.apkg("modern_anki21b_main.apkg", "default") }

    init {
        rig.installOn(this)

        context("미리보기로 가는 길") {
            test("텍스트 파일을 다 읽으면 기본 카드 구성으로 만든 미리보기로 간다") {
                runTest(rig.dispatcher) {
                    val table = ImportTestTables.text(sourceName = "words.txt", rows = 3)
                    val harness = picked("words.txt", FakeImportSourceReader().returns(table))

                    val state = harness.state
                    state.step shouldBe ImportStep.Preview
                    state.loading shouldBe null
                    state.table shouldBe table
                    state.mappings shouldBe defaultFieldMappings(table)
                    state.preview
                        .shouldNotBeNull()
                        .cards.size shouldBe 3
                    state.previewFilter shouldBe ImportPreviewFilter.All
                    state.filteredCards shouldBe state.preview?.cards
                    state.filterCounts[ImportPreviewFilter.All] shouldBe 3
                    state.deckName shouldBe "words"
                }
            }

            test("엑셀 파일을 다 읽으면 첫 시트로 미리보기로 가고 덱 이름에 시트 이름을 잇는다") {
                runTest(rig.dispatcher) {
                    val harness = picked("book.xlsx", FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(3, 2))))

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.selection.sheetIndex shouldBe 0
                    harness.state.deckName shouldBe "book - 시트1"
                    harness.reader.calls.size shouldBe 1
                }
            }

            test("Anki 파일은 덮개를 유지한 채 모든 하위 덱의 행을 한 번 더 읽는다") {
                runTest(rig.dispatcher) {
                    val reader = FakeImportSourceReader().returns(ankiSummary)
                    reader.suspends()
                    val harness = picked("deck.apkg", reader)

                    harness.state.loading shouldBe ImportLoading.ReadingFile("deck.apkg")
                    harness.state.step shouldBe ImportStep.Source
                    val calls = harness.reader.calls.map { call -> call.shouldBeInstanceOf<ReadCall.FileRead>() }
                    calls.map { call -> call.subdeckIds } shouldBe listOf(emptySet(), null)
                }
            }

            test("Anki 하위 덱 행을 다 읽으면 그 표와 모든 하위 덱을 고른 선택으로 미리보기로 간다") {
                runTest(rig.dispatcher) {
                    val harness = picked("deck.apkg", FakeImportSourceReader().returns(ankiSummary).returns(ankiFull))

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.table shouldBe ankiFull
                    harness.state.selection.subdeckIds shouldBe ankiFull.subdecks.map { subdeck -> subdeck.id }.toSet()
                    harness.state.preview
                        .shouldNotBeNull()
                        .cards
                        .isNotEmpty() shouldBe true
                }
            }

            test("엑셀 첫 시트에 데이터가 없고 후보가 있으면 그 시트를 다시 읽어 미리보기로 간다") {
                runTest(rig.dispatcher) {
                    val second = ImportTestTables.xlsx(sheetRows = listOf(0, 3), selectedSheet = 1)
                    val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(0, 3))).returns(second)
                    val harness = picked("book.xlsx", reader)

                    harness.reader.calls.map { call -> (call as ReadCall.FileRead).sheetIndex } shouldBe listOf(0, 1)
                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.table shouldBe second
                    harness.state.selection.sheetIndex shouldBe 1
                    harness.state.deckName shouldBe "book - 시트2"
                }
            }

            test("다시 읽은 후보 시트가 0행이면 다음 후보 시트로 넘어간다") {
                runTest(rig.dispatcher) {
                    val third = ImportTestTables.xlsx(sheetRows = listOf(0, 2, 3), selectedSheet = 2)
                    val reader =
                        FakeImportSourceReader()
                            .returns(ImportTestTables.xlsx(sheetRows = listOf(0, 2, 3)))
                            .returns(ImportTestTables.xlsx(sheetRows = listOf(0, 0, 3), selectedSheet = 1))
                            .returns(third)
                    val harness = picked("book.xlsx", reader)

                    harness.reader.calls.map { call -> (call as ReadCall.FileRead).sheetIndex } shouldBe listOf(0, 1, 2)
                    harness.state.table shouldBe third
                    harness.state.selection.sheetIndex shouldBe 2
                }
            }

            test("실제 파서로 읽은 텍스트 파일은 미리보기로 간다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = rig.realParserReader())

                    harness.pick(FakePickedImportFile(name = "words.txt", content = "学校\t학교\n先生\t선생님".encodeUtf8()))
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.preview
                        .shouldNotBeNull()
                        .cards
                        .map { card -> card.front } shouldBe listOf("学校", "先生")
                }
            }
        }

        context("카드가 없을 때") {
            test("Image Occlusion만 있으면 전용 본문의 카드 없음 대화상자를 띄우고 Source로 간다") {
                runTest(rig.dispatcher) {
                    val summary = ImportTestTables.apkgSummary("modern_image_occlusion.apkg")
                    val harness = picked("io.apkg", FakeImportSourceReader().returns(summary).returns(imageOcclusionOnly()))

                    harness.state.dialog shouldBe ImportDialog.NoCards(NoCardsKind.ImageOcclusionOnly)
                    harness.state.step shouldBe ImportStep.Source
                    harness.state.table shouldBe null
                    harness.tempStorage.regularFiles().shouldBeEmpty()
                }
            }

            test("카드로 만들 내용이 없으면 카드 없음 대화상자를 띄우고 [확인]을 누르면 Source에 머문다") {
                runTest(rig.dispatcher) {
                    val harness = picked("blank.txt", FakeImportSourceReader().returns(ImportTestTables.blankText()))
                    harness.state.dialog shouldBe ImportDialog.NoCards(NoCardsKind.NoContent)

                    harness.send(ImportDeckIntent.DismissDialog)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe ImportStep.Source
                    harness.tempStorage.regularFiles().shouldBeEmpty()
                }
            }

            test("데이터 행이 있는 후보 시트가 없으면 카드 없음 대화상자를 띄운다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader()
                            .returns(ImportTestTables.xlsx(sheetRows = listOf(0, 2)))
                            .returns(ImportTestTables.xlsx(sheetRows = listOf(0, 0), selectedSheet = 1))
                    val harness = picked("book.xlsx", reader)

                    harness.state.dialog shouldBe ImportDialog.NoCards(NoCardsKind.NoContent)
                    harness.state.step shouldBe ImportStep.Source
                }
            }
        }

        context("카드가 너무 많을 때") {
            val limits = ImportLimitsConfig(maxCards = 3)

            test("텍스트 파일이 상한을 넘으면 너무 많다는 대화상자를 띄우고 [확인]을 누르면 Source에 머문다") {
                runTest(rig.dispatcher) {
                    val harness = picked("words.txt", FakeImportSourceReader().returns(ImportTestTables.text(rows = 4)), limits)
                    harness.state.dialog shouldBe ImportDialog.TooManyCards(isPaste = false)
                    harness.state.step shouldBe ImportStep.Source
                    harness.tempStorage.regularFiles().shouldBeEmpty()

                    harness.send(ImportDeckIntent.DismissDialog)
                    advanceUntilIdle()

                    harness.state.dialog shouldBe null
                    harness.state.step shouldBe ImportStep.Source
                }
            }

            test("다시 읽은 후보 시트가 상한을 넘으면 너무 많다는 대화상자다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader()
                            .returns(ImportTestTables.xlsx(sheetRows = listOf(0, 4)))
                            .returns(ImportTestTables.xlsx(sheetRows = listOf(0, 4), selectedSheet = 1))
                    val harness = picked("book.xlsx", reader, limits)

                    harness.state.dialog shouldBe ImportDialog.TooManyCards(isPaste = false)
                    harness.state.step shouldBe ImportStep.Source
                }
            }
        }
    }

    private fun TestScope.picked(
        fileName: String,
        reader: FakeImportSourceReader,
        limits: ImportLimitsConfig = ImportLimitsConfig(),
    ): ImportViewModelHarness {
        val harness = rig.create(reader = reader, limits = limits)
        harness.pickNamed(fileName)
        advanceUntilIdle()
        return harness
    }

    private fun imageOcclusionOnly(): ParsedTable {
        val anki = ImportTestTables.apkg("modern_image_occlusion.apkg", "default")
        return anki.copy(noteTypes = anki.noteTypes.filter { type -> type.isImageOcclusion }, rows = emptyList())
    }
}
