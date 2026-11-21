package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.mvi.DeckInfoDraft
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.source.ImportLimitsConfig
import com.whatever.caro.feature.importer.support.FakeImportSourceReader
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.ImportViewModelHarness
import com.whatever.caro.feature.importer.support.ImportViewModelRig
import com.whatever.caro.feature.importer.support.pickedFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/** 덱 정보: 첫 미리보기의 미리 채우기, 미리보기 위 시트의 열기와 [적용]과 닫기, 설정을 바꾼 뒤 다시 채우기를 확인한다. */
class ImportDeckViewModelDeckInfoTest : FunSpec() {
    private val rig = ImportViewModelRig()

    init {
        rig.installOn(this)

        context("미리 채우기") {
            test("붙여넣기는 넘겨받은 덱 이름과 설명 기본값을 쓴다") {
                runTest(rig.dispatcher) {
                    val harness = rig.create(reader = FakeImportSourceReader().returns(pasteTable()))
                    harness.pasteNext("a\tb")
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.deckName shouldBe ImportViewModelHarness.PASTE_SOURCE_NAME
                    harness.state.deckDescription shouldBe ImportViewModelHarness.PASTE_DESCRIPTION
                }
            }

            test("먼저 고칠 것 화면을 거쳐 처음 미리보기를 열어도 이름과 설명을 채운다") {
                runTest(rig.dispatcher) {
                    val second = ImportTestTables.xlsx(sheetRows = listOf(5, 2), selectedSheet = 1)
                    val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(5, 2))).returns(second)
                    val harness = pickedFile(rig, "book.xlsx", reader, ImportLimitsConfig(maxCards = 3))
                    harness.state.step shouldBe ImportStep.Settings(SettingsKind.Sheet, SettingsEntry.Gate)

                    harness.send(ImportDeckIntent.SelectSheet(1))
                    advanceUntilIdle()
                    harness.send(ImportDeckIntent.ClickApplySettings)
                    advanceUntilIdle()

                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.deckName shouldBe "book - 시트2"
                    harness.state.deckDescription shouldBe "book.xlsx에서 가져왔어요"
                }
            }
        }

        context("덱 정보 시트") {
            test("덱 이름 줄 [변경]을 누르면 지금 이름과 설명으로 시트를 연다") {
                runTest(rig.dispatcher) {
                    val harness = preview("words.txt", ImportTestTables.text())

                    harness.send(ImportDeckIntent.ClickChangeDeckInfo)
                    advanceUntilIdle()

                    harness.state.deckInfoDraft shouldBe DeckInfoDraft(name = "words", description = "words.txt에서 가져왔어요")
                    harness.state.step shouldBe ImportStep.Preview
                }
            }

            test("[적용]하면 시트를 닫고 덱 이름과 설명을 바꾼다") {
                runTest(rig.dispatcher) {
                    val harness = editing("words.txt")

                    harness.send(ImportDeckIntent.UpdateDeckInfoName("TOEIC 3000"))
                    harness.send(ImportDeckIntent.UpdateDeckInfoDescription("영단어 모음"))
                    harness.send(ImportDeckIntent.ApplyDeckInfo)
                    advanceUntilIdle()

                    harness.state.deckInfoDraft shouldBe null
                    harness.state.deckName shouldBe "TOEIC 3000"
                    harness.state.deckDescription shouldBe "영단어 모음"
                    harness.state.isDeckInfoEdited shouldBe true
                }
            }

            test("바깥을 누르거나 끌어내려 닫으면 바꾸지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = editing("words.txt")
                    harness.send(ImportDeckIntent.UpdateDeckInfoName("TOEIC 3000"))

                    harness.send(ImportDeckIntent.DismissDeckInfo)
                    advanceUntilIdle()

                    harness.state.deckInfoDraft shouldBe null
                    harness.state.deckName shouldBe "words"
                    harness.state.isDeckInfoEdited shouldBe false
                }
            }

            test("시트가 열린 동안 뒤로 가기는 바꾸지 않고 시트만 닫는다") {
                runTest(rig.dispatcher) {
                    val harness = editing("words.txt")
                    harness.send(ImportDeckIntent.UpdateDeckInfoDescription("영단어 모음"))

                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()

                    harness.state.deckInfoDraft shouldBe null
                    harness.state.deckDescription shouldBe "words.txt에서 가져왔어요"
                    harness.state.step shouldBe ImportStep.Preview
                    harness.state.dialog shouldBe null
                }
            }

            test("이름이나 설명이 공백뿐이면 [적용]이 꺼지고 눌러도 바뀌지 않는다") {
                runTest(rig.dispatcher) {
                    val harness = editing("words.txt")

                    harness.send(ImportDeckIntent.UpdateDeckInfoName("   "))
                    advanceUntilIdle()
                    harness.state.deckInfoDraft
                        .shouldNotBeNull()
                        .isApplyEnabled shouldBe false
                    harness.send(ImportDeckIntent.UpdateDeckInfoName("TOEIC"))
                    harness.send(ImportDeckIntent.UpdateDeckInfoDescription(""))
                    advanceUntilIdle()
                    harness.state.deckInfoDraft
                        .shouldNotBeNull()
                        .isApplyEnabled shouldBe false

                    harness.send(ImportDeckIntent.ApplyDeckInfo)
                    advanceUntilIdle()

                    harness.state.deckInfoDraft.shouldNotBeNull()
                    harness.state.deckName shouldBe "words"
                    harness.state.isDeckInfoEdited shouldBe false
                }
            }

            test("이름은 50자, 설명은 500자에서 자른다") {
                runTest(rig.dispatcher) {
                    val harness = editing("words.txt")

                    harness.send(ImportDeckIntent.UpdateDeckInfoName("a".repeat(60)))
                    harness.send(ImportDeckIntent.UpdateDeckInfoDescription("b".repeat(600)))
                    advanceUntilIdle()

                    val draft = harness.state.deckInfoDraft.shouldNotBeNull()
                    draft.name shouldBe "a".repeat(50)
                    draft.description shouldBe "b".repeat(500)
                }
            }

            test("시트가 닫혀 있으면 입력과 [적용]을 무시한다") {
                runTest(rig.dispatcher) {
                    val harness = preview("words.txt", ImportTestTables.text())
                    val before = harness.state

                    harness.send(ImportDeckIntent.UpdateDeckInfoName("TOEIC"))
                    harness.send(ImportDeckIntent.UpdateDeckInfoDescription("영단어"))
                    harness.send(ImportDeckIntent.ApplyDeckInfo)
                    advanceUntilIdle()

                    harness.state shouldBe before
                }
            }

            test("미리보기가 아니거나 카드 전체 보기가 떠 있으면 시트를 열지 않는다") {
                runTest(rig.dispatcher) {
                    val source = rig.create()
                    source.send(ImportDeckIntent.ClickChangeDeckInfo)
                    val harness = preview("words.txt", ImportTestTables.text())
                    harness.send(ImportDeckIntent.ClickCard(harness.state.filteredCards.first()))
                    harness.send(ImportDeckIntent.ClickChangeDeckInfo)
                    advanceUntilIdle()

                    source.state.deckInfoDraft shouldBe null
                    harness.state.deckInfoDraft shouldBe null
                }
            }

            test("시트가 열린 동안에는 설정 [변경]과 카드 누름을 무시한다") {
                runTest(rig.dispatcher) {
                    val harness = editing("words.txt")
                    val before = harness.state

                    harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Delimiter))
                    harness.send(ImportDeckIntent.ClickCard(before.filteredCards.first()))
                    advanceUntilIdle()

                    harness.state shouldBe before
                }
            }
        }

        context("설정을 바꾼 뒤 다시 채우기") {
            test("미리 채운 이름과 같은 이름을 [적용]해도 시트를 바꾸면 그 이름을 그대로 둔다") {
                runTest(rig.dispatcher) {
                    val second = ImportTestTables.xlsx(sheetRows = listOf(3, 2), selectedSheet = 1)
                    val reader = FakeImportSourceReader().returns(ImportTestTables.xlsx(sheetRows = listOf(3, 2))).returns(second)
                    val harness = preview("book.xlsx", reader = reader)
                    harness.send(ImportDeckIntent.ClickChangeDeckInfo)
                    harness.send(ImportDeckIntent.ApplyDeckInfo)
                    advanceUntilIdle()
                    harness.state.deckName shouldBe "book - 시트1"

                    changeSheet(harness, index = 1)

                    harness.state.selection.sheetIndex shouldBe 1
                    harness.state.deckName shouldBe "book - 시트1"
                    harness.state.deckDescription shouldBe "book.xlsx에서 가져왔어요"
                }
            }

            test("처음으로 돌아가 새 파일을 읽으면 적용한 덱 정보를 버리고 새 파일로 다시 채운다") {
                runTest(rig.dispatcher) {
                    val reader =
                        FakeImportSourceReader()
                            .returns(
                                ImportTestTables.text(),
                            ).returns(ImportTestTables.text(sourceName = "verbs.txt"))
                    val harness = preview("words.txt", reader = reader)
                    harness.send(ImportDeckIntent.ClickChangeDeckInfo)
                    harness.send(ImportDeckIntent.UpdateDeckInfoName("내 단어장"))
                    harness.send(ImportDeckIntent.ApplyDeckInfo)
                    harness.send(ImportDeckIntent.ClickBack)
                    advanceUntilIdle()
                    harness.state.dialog shouldBe ImportDialog.DiscardConfirm

                    harness.send(ImportDeckIntent.ConfirmDiscard)
                    advanceUntilIdle()
                    harness.state.isDeckInfoEdited shouldBe false
                    harness.pickNamed("verbs.txt")
                    advanceUntilIdle()

                    harness.state.deckName shouldBe "verbs"
                    harness.state.deckDescription shouldBe "verbs.txt에서 가져왔어요"
                }
            }
        }
    }

    private fun TestScope.preview(
        fileName: String,
        table: ParsedTable? = null,
        reader: FakeImportSourceReader = FakeImportSourceReader().returns(table ?: ImportTestTables.text(sourceName = fileName)),
    ): ImportViewModelHarness {
        val harness = pickedFile(rig, fileName, reader)
        harness.state.step shouldBe ImportStep.Preview
        return harness
    }

    private fun TestScope.editing(fileName: String): ImportViewModelHarness {
        val harness = preview(fileName, ImportTestTables.text(sourceName = fileName))
        harness.send(ImportDeckIntent.ClickChangeDeckInfo)
        advanceUntilIdle()
        harness.state.deckInfoDraft.shouldNotBeNull()
        return harness
    }

    private fun TestScope.changeSheet(
        harness: ImportViewModelHarness,
        index: Int,
    ) {
        harness.send(ImportDeckIntent.ClickChangeSetting(SettingsKind.Sheet))
        harness.send(ImportDeckIntent.SelectSheet(index))
        advanceUntilIdle()
        harness.send(ImportDeckIntent.ClickApplySettings)
        advanceUntilIdle()
        harness.state.step shouldBe ImportStep.Preview
    }

    private fun pasteTable() = ImportTestTables.text(sourceName = ImportViewModelHarness.PASTE_SOURCE_NAME)
}
