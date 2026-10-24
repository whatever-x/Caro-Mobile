package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog
import com.whatever.caro.feature.importer.mvi.ImportLoading
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SavingProgress
import com.whatever.caro.feature.importer.platform.PickResult
import com.whatever.caro.feature.importer.support.ImportTestTables
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentSetOf
import okio.IOException

class ImportStepRulesTest : FunSpec() {
    private val modernSummary by lazy { ImportTestTables.apkgSummary("modern_anki21b_main.apkg") }
    private val edgeSummary by lazy { ImportTestTables.apkgSummary("legacy_edge_cases.apkg") }

    init {
        context("바쁜 동안 입력 무시") {
            val allowedWhileBusy =
                listOf(
                    ImportDeckIntent.ClickBack,
                    ImportDeckIntent.DismissDialog,
                    ImportDeckIntent.FilePicked(PickResult.Cancelled, SpreadsheetDateStyle.YEAR_MONTH_DAY),
                )

            val saveChoices =
                listOf(
                    ImportDeckIntent.ClickStop,
                    ImportDeckIntent.ConfirmStop,
                    ImportDeckIntent.RetrySave,
                    ImportDeckIntent.ClickViewDeck,
                    ImportDeckIntent.ClickGoHome,
                )

            test("바쁘지 않으면 아무 입력도 무시하지 않는다") {
                isIgnoredWhileBusy(ImportDeckState(), ImportDeckIntent.ClickPickFile) shouldBe false
                isIgnoredWhileBusy(ImportDeckState(), ImportDeckIntent.ClickSelectionNext) shouldBe false
            }

            test("읽는 중에도 뒤로 가기, 대화상자 닫기, 선택기 결과는 받는다") {
                val reading = ImportDeckState(loading = ImportLoading.Parsing("deck.apkg"))

                allowedWhileBusy.forEach { intent -> isIgnoredWhileBusy(reading, intent) shouldBe false }
            }

            test("선택기가 열려 있으면 붙여넣기 같은 새 입력은 무시한다") {
                isIgnoredWhileBusy(ImportDeckState(isPickerOpen = true), ImportDeckIntent.ClickPaste) shouldBe true
            }

            test("다시 읽는 중에는 구분자와 제목 행 변경도 무시한다") {
                val reparsing = ImportDeckState(loading = ImportLoading.Reparsing("deck.apkg"))

                isIgnoredWhileBusy(reparsing, ImportDeckIntent.SelectDelimiter(',')) shouldBe true
                isIgnoredWhileBusy(reparsing, ImportDeckIntent.ChangeHeaderRow(true)) shouldBe true
            }

            test("저장 중에는 멈춤과 실패 대화상자의 선택도 받는다") {
                val saving = ImportDeckState(step = ImportStep.Saving, saving = SavingProgress(deckName = "deck", totalCards = 3))

                saveChoices.forEach { intent -> isIgnoredWhileBusy(saving, intent) shouldBe false }
                isIgnoredWhileBusy(saving, ImportDeckIntent.ClickCreateDeck) shouldBe true
            }

            test("덱을 만드는 중에는 저장 선택을 무시한다") {
                val creating = ImportDeckState(step = ImportStep.CreatingDeck)

                saveChoices.forEach { intent -> isIgnoredWhileBusy(creating, intent) shouldBe true }
            }

            test("텍스트를 다시 나누는 중에는 구분자와 제목 행 변경만 이어서 받는다") {
                val textReparsing = ImportDeckState(loading = ImportLoading.TextReparsing)

                isIgnoredWhileBusy(textReparsing, ImportDeckIntent.SelectDelimiter(',')) shouldBe false
                isIgnoredWhileBusy(textReparsing, ImportDeckIntent.ChangeHeaderRow(true)) shouldBe false
                isIgnoredWhileBusy(textReparsing, ImportDeckIntent.ClickSelectionNext) shouldBe true
            }
        }

        context("빈 결과 판정") {
            test("하위 덱도 덱 없음 노트도 없는 apkg는 비었다") {
                isEmptyImport(ImportTestTables.apkgSummary("empty_zero_notes.apkg")) shouldBe true
            }

            test("하위 덱이 있는 apkg는 비지 않았다") {
                isEmptyImport(modernSummary) shouldBe false
            }

            test("모든 시트가 비어 있는 xlsx만 비었다") {
                isEmptyImport(ImportTestTables.xlsx(sheetRows = listOf(0, 0))) shouldBe true
                isEmptyImport(ImportTestTables.xlsx(sheetRows = listOf(0, 2))) shouldBe false
            }

            test("텍스트는 노트 유형이 없을 때만 비었고 제목 줄만 있는 입력은 비지 않았다") {
                isEmptyImport(ImportTestTables.blankText()) shouldBe true
                isEmptyImport(ImportTestTables.titleOnlyText()) shouldBe false
            }
        }

        context("읽기 실패 대화상자") {
            test("파서 오류는 종류와 확장자를 그대로 대화상자에 담는다") {
                val error = ImportParseException(ImportParseErrorKind.CORRUPT_FILE, "forced")

                readFailureDialog(error, "xlsx", isPaste = false) shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, "xlsx", isPaste = false)
            }

            test("파일이 너무 크다는 파서 오류는 크기 초과 대화상자가 된다") {
                val error = ImportParseException(ImportParseErrorKind.FILE_TOO_LARGE, "forced")

                readFailureDialog(error, "apkg", isPaste = false) shouldBe ImportDialog.FileTooLarge("apkg")
            }

            test("파서 밖의 읽기 실패는 파일을 읽지 못했다는 문구가 나오는 종류로 보낸다") {
                readFailureDialog(IOException("disk"), "txt", isPaste = false) shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, "txt", isPaste = false)
                readFailureDialog(IllegalStateException("boom"), "", isPaste = true) shouldBe
                    ImportDialog.ParseError(ImportParseErrorKind.ARCHIVE_LIMIT_EXCEEDED, "", isPaste = true)
            }

            test("닫으면 읽은 내용을 버리는 대화상자는 읽기 실패 계열뿐이다") {
                ImportDialog.FileTooLarge("apkg").isReadFailure() shouldBe true
                ImportDialog.ParseError(ImportParseErrorKind.CORRUPT_FILE, "apkg", isPaste = false).isReadFailure() shouldBe true
                ImportDialog.EmptyResult(isPaste = false).isReadFailure() shouldBe true
                ImportDialog.DiscardConfirm.isReadFailure() shouldBe false
            }
        }

        context("하위 덱 선택") {
            test("목록에 있는 하위 덱 id는 고를 수 있고 모르는 id는 고를 수 없다") {
                isSelectableSubdeck(modernSummary, modernSummary.subdecks.first().id) shouldBe true
                isSelectableSubdeck(modernSummary, "unknown") shouldBe false
            }

            test("덱 없음은 카드 없는 노트가 있을 때만 고를 수 있다") {
                isSelectableSubdeck(edgeSummary, ParsedSubdeck.NO_DECK_ID) shouldBe true
                isSelectableSubdeck(modernSummary, ParsedSubdeck.NO_DECK_ID) shouldBe false
            }

            test("일부만 고른 상태에서 전체 선택을 누르면 모든 하위 덱을 고른다") {
                val ids = modernSummary.subdecks.map { subdeck -> subdeck.id }
                val partial = persistentSetOf(ids.first())

                toggleAllSubdecks(modernSummary, partial) shouldBe ids.toSet()
            }

            test("전체 선택을 누르면 덱 없음 선택을 남긴 채 하위 덱만 푼다") {
                val all = modernSummary.subdecks.map { subdeck -> subdeck.id }.toSet() + ParsedSubdeck.NO_DECK_ID

                toggleAllSubdecks(modernSummary, persistentSetOf<String>().addAll(all)) shouldBe setOf(ParsedSubdeck.NO_DECK_ID)
            }
        }

        context("시트 비어 있음") {
            test("고른 시트의 행이 0이면 비었다") {
                val table = ImportTestTables.xlsx(sheetRows = listOf(3, 0))

                isSheetEmpty(table, sheetIndex = 0) shouldBe false
                isSheetEmpty(table, sheetIndex = 1) shouldBe true
            }

            test("목록에 없는 시트 번호는 비었다고 보지 않는다") {
                isSheetEmpty(ImportTestTables.xlsx(sheetRows = listOf(3, 0)), sheetIndex = 9) shouldBe false
            }
        }
    }
}
