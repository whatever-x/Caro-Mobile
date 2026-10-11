package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.FakePickedImportFile
import com.whatever.caro.feature.importer.support.ImportMappingTables
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.PASTE_DESCRIPTION
import com.whatever.caro.feature.importer.support.openDeckInfo
import com.whatever.caro.feature.importer.support.openMappingFromFile
import com.whatever.caro.feature.importer.support.openMappingFromPaste
import com.whatever.caro.feature.importer.support.openPreview
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 덱 정보 단계: 이름과 설명 미리 채움, 입력 길이, 뒤로 가기에서의 입력 유지를 확인한다. */
class ImportDeckViewModelDeckInfoTest : FunSpec() {
    private val rig = ImportViewModelRig()

    private class PrefillCase(
        val label: String,
        val fileName: String,
        val table: ParsedTable,
        val name: String,
        val description: String,
    )

    init {
        rig.installOn(this)

        context("미리 채움") {
            listOf(
                PrefillCase(
                    "apkg는 고른 하위 덱의 공통 상위 덱 이름과 파일 이름을 넣은 설명이다",
                    "TOEIC 3000.apkg",
                    ImportMappingTables.deckTable("TOEIC 3000.apkg", "TOEIC::Day 01", "TOEIC::Day 02"),
                    "TOEIC",
                    "TOEIC 3000.apkg에서 가져왔어요",
                ),
                PrefillCase(
                    "apkg는 공통 상위가 없고 최상위 덱이 하나면 그 덱 이름이다",
                    "jlpt.apkg",
                    ImportMappingTables.deckTable("jlpt.apkg", "JLPT N5"),
                    "JLPT N5",
                    "jlpt.apkg에서 가져왔어요",
                ),
                PrefillCase(
                    "apkg는 최상위 덱이 여럿이면 확장자를 뺀 파일 이름이다",
                    "My Decks.apkg",
                    ImportMappingTables.deckTable("My Decks.apkg", "Japanese::N5", "Korean::TOPIK"),
                    "My Decks",
                    "My Decks.apkg에서 가져왔어요",
                ),
                PrefillCase(
                    "텍스트 파일은 확장자를 뺀 파일 이름이다",
                    "words.txt",
                    ImportTestTables.text(sourceName = "words.txt"),
                    "words",
                    "words.txt에서 가져왔어요",
                ),
            ).forEach { case ->
                test(case.label) {
                    runTest(rig.dispatcher) {
                        val harness = fileDeckInfo(case.fileName, case.table)

                        harness.state.step shouldBe ImportStep.DeckInfo
                        harness.state.deckName shouldBe case.name
                        harness.state.deckDescription shouldBe case.description
                    }
                }
            }

            test("엑셀은 시트가 하나면 확장자를 뺀 파일 이름이다") {
                runTest(rig.dispatcher) {
                    val harness = fileDeckInfo("단어장.xlsx", ImportTestTables.xlsx(sourceName = "단어장.xlsx", sheetRows = listOf(3)))

                    harness.state.deckName shouldBe "단어장"
                    harness.state.deckDescription shouldBe "단어장.xlsx에서 가져왔어요"
                }
            }

            test("엑셀은 시트가 여러 개면 파일 이름과 고른 시트 이름을 ' - '로 잇는다") {
                runTest(rig.dispatcher) {
                    val harness = fileDeckInfo("단어장.xlsx", ImportTestTables.xlsx(sourceName = "단어장.xlsx", sheetRows = listOf(3, 2, 0)))

                    harness.state.deckName shouldBe "단어장 - 시트1"
                }
            }

            test("붙여넣기는 넘겨받은 sourceName과 고정 설명이다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text(sourceName = PASTE_NAME)))
                    openMappingFromPaste(harness, "apple\t사과")
                    openPreview(harness)

                    openDeckInfo(harness)

                    harness.state.deckName shouldBe PASTE_NAME
                    harness.state.deckDescription shouldBe PASTE_DESCRIPTION
                }
            }

            test("긴 파일 이름은 이름을 50자에서 자른다") {
                runTest(rig.dispatcher) {
                    val longName = "a".repeat(80) + ".txt"

                    val harness = fileDeckInfo(longName, ImportTestTables.text(sourceName = longName))

                    harness.state.deckName shouldBe "a".repeat(50)
                }
            }

            test("긴 설명 템플릿은 설명을 500자에서 자른다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(ImportTestTables.text()))
                    openMappingFromFile(harness, "words.txt")
                    openPreview(harness)

                    harness.send(ImportDeckIntent.ClickPreviewNext("x".repeat(600) + "{file}", PASTE_DESCRIPTION))
                    advanceUntilIdle()

                    harness.state.deckDescription shouldBe "x".repeat(500)
                }
            }
        }

        context("읽은 원본마다 한 번만 채운다") {
            test("이름을 고치고 Preview로 갔다가 돌아와도 고친 이름이 남는다") {
                runTest(rig.dispatcher) {
                    val harness = textDeckInfo()
                    harness.send(ImportDeckIntent.UpdateDeckName("내가 고친 이름"))
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    harness.state.step shouldBe ImportStep.Preview

                    openDeckInfo(harness)

                    harness.state.deckName shouldBe "내가 고친 이름"
                    harness.state.deckDescription shouldBe "words.txt에서 가져왔어요"
                }
            }

            test("파일을 버리고 새 파일을 읽으면 이름과 설명을 새 파일 기준으로 다시 채운다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader().returns(ImportTestTables.text()).returns(ImportTestTables.text(sourceName = "other.txt"))
                    val harness = rig.create(reader = reader)
                    openMappingFromFile(harness, "words.txt")
                    openPreview(harness)
                    openDeckInfo(harness)
                    harness.send(ImportDeckIntent.UpdateDeckName("내가 고친 이름"))
                    repeat(4) { harness.send(ImportDeckIntent.ClickBack) }
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ConfirmDiscard)
                    advanceUntilIdle()

                    openMappingFromFile(harness, "other.txt")
                    openPreview(harness)
                    openDeckInfo(harness)

                    harness.state.deckName shouldBe "other"
                    harness.state.deckDescription shouldBe "other.txt에서 가져왔어요"
                }
            }

            test("붙여넣기를 다시 읽으면 고친 이름이 사라지고 다시 채운다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader()
                            .returns(ImportTestTables.text(sourceName = PASTE_NAME))
                            .returns(ImportTestTables.text(sourceName = PASTE_NAME))
                    val harness = rig.create(reader = reader)
                    openMappingFromPaste(harness, "apple\t사과")
                    openPreview(harness)
                    openDeckInfo(harness)
                    harness.send(ImportDeckIntent.UpdateDeckName("내가 고친 이름"))
                    repeat(4) { harness.send(ImportDeckIntent.ClickBack) }
                    advanceUntilIdle()
                    harness.state.step shouldBe ImportStep.Paste
                    harness.send(ImportDeckIntent.ClickPasteNext(PASTE_NAME, SpreadsheetDateStyle.YEAR_MONTH_DAY))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ClickSelectionNext)
                    advanceUntilIdle()
                    openPreview(harness)

                    openDeckInfo(harness)

                    harness.state.deckName shouldBe PASTE_NAME
                }
            }
        }

        context("입력") {
            test("UpdateDeckName은 50자에서 자른다") {
                runTest(rig.dispatcher) {
                    val harness = textDeckInfo()

                    harness.send(ImportDeckIntent.UpdateDeckName("n".repeat(60)))
                    advanceUntilIdle()

                    harness.state.deckName shouldBe "n".repeat(50)
                }
            }

            test("UpdateDeckDescription은 500자에서 자른다") {
                runTest(rig.dispatcher) {
                    val harness = textDeckInfo()

                    harness.send(ImportDeckIntent.UpdateDeckDescription("d".repeat(600)))
                    advanceUntilIdle()

                    harness.state.deckDescription shouldBe "d".repeat(500)
                }
            }

            test("이름이 공백이면 [가져오기]를 끈다") {
                runTest(rig.dispatcher) {
                    val harness = textDeckInfo()
                    harness.state.isDeckInfoConfirmEnabled shouldBe true

                    harness.send(ImportDeckIntent.UpdateDeckName("   "))
                    advanceUntilIdle()

                    harness.state.isDeckInfoConfirmEnabled shouldBe false
                }
            }

            test("설명이 비어 있으면 [가져오기]를 끈다") {
                runTest(rig.dispatcher) {
                    val harness = textDeckInfo()
                    harness.state.isDeckInfoConfirmEnabled shouldBe true

                    harness.send(ImportDeckIntent.UpdateDeckDescription(""))
                    advanceUntilIdle()

                    harness.state.isDeckInfoConfirmEnabled shouldBe false
                }
            }
        }

        context("뒤로 가기") {
            test("DeckInfo에서 뒤로 가면 Preview로 가고 입력을 유지한다") {
                runTest(rig.dispatcher) {
                    val harness = textDeckInfo()
                    harness.send(ImportDeckIntent.UpdateDeckName("내가 고친 이름"))
                    harness.send(ImportDeckIntent.UpdateDeckDescription("내가 고친 설명"))

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.dialog shouldBe null
                    harness.state.deckName shouldBe "내가 고친 이름"
                    harness.state.deckDescription shouldBe "내가 고친 설명"
                }
            }
        }
    }

    private fun TestScope.fileDeckInfo(
        fileName: String,
        table: ParsedTable,
    ): ImportViewModelHarness {
        val reader =
            if (table.subdecks.isEmpty()) {
                FakeImportSourceReader().returns(table)
            } else {
                FakeImportSourceReader().returns(ImportMappingTables.summaryOf(table)).returns(table)
            }
        val harness = rig.create(reader = reader)
        harness.pick(FakePickedImportFile.ofSize(fileName, size = 100))
        advanceUntilIdle()
        harness.send(ImportDeckIntent.ClickSelectionNext)
        advanceUntilIdle()
        openPreview(harness)
        openDeckInfo(harness)
        return harness
    }

    private fun TestScope.textDeckInfo(): ImportViewModelHarness = fileDeckInfo("words.txt", ImportTestTables.text())

    private companion object {
        const val PASTE_NAME = ImportViewModelHarness.PASTE_SOURCE_NAME
    }
}
