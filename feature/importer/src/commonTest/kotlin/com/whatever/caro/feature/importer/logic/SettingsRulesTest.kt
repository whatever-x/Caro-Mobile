package com.whatever.caro.feature.importer.logic

import androidx.compose.ui.state.ToggleableState
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.mvi.SettingsReason
import com.whatever.caro.feature.importer.support.ImportTestTables
import com.whatever.caro.feature.importer.support.PreviewSamples
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.collections.immutable.persistentSetOf

class SettingsRulesTest : FunSpec() {
    private val subdeckTable =
        ImportTestTables.text().copy(
            format = ImportFormat.APKG,
            subdecks =
                listOf(
                    ParsedSubdeck(id = "a", path = "Deck::A", noteCount = 4, imageOcclusionNoteCount = 1),
                    ParsedSubdeck(id = "b", path = "Deck::B", noteCount = 2),
                    ParsedSubdeck(id = "c", path = "Deck::C", noteCount = 2, imageOcclusionNoteCount = 2),
                ),
        )

    init {
        context("설정 화면을 열 수 있는지") {
            test("하위 덱은 Anki, 시트는 엑셀, 구분 기준은 텍스트와 붙여넣기에서만 연다") {
                val anki = ImportTestTables.apkg("modern_anki21b_main.apkg", "default")
                val xlsx = ImportTestTables.xlsx()
                val text = ImportTestTables.text()

                SettingsKind.entries.filter { kind -> isSettingsKindAvailable(kind, anki) } shouldBe
                    listOf(SettingsKind.Subdeck, SettingsKind.CardLayout)
                SettingsKind.entries.filter { kind -> isSettingsKindAvailable(kind, xlsx) } shouldBe
                    listOf(SettingsKind.Sheet, SettingsKind.CardLayout)
                SettingsKind.entries.filter { kind -> isSettingsKindAvailable(kind, text) } shouldBe
                    listOf(SettingsKind.Delimiter, SettingsKind.CardLayout)
            }

            test("가져올 수 있는 유형이 없으면 카드 구성을 열지 않는다") {
                isSettingsKindAvailable(SettingsKind.CardLayout, ImportTestTables.blankText()) shouldBe false
            }
        }

        context("하위 덱") {
            test("고른 장수는 Image Occlusion 노트를 빼고 센다") {
                selectedSubdeckCardCount(subdeckTable, setOf("a", "b", "c")) shouldBe 5
                selectedSubdeckCardCount(subdeckTable, setOf("a")) shouldBe 3
            }

            test("표시 이름은 모든 하위 덱의 공통 상위 경로를 뺀 나머지다") {
                subdeckDisplayNames(subdeckTable) shouldBe listOf("A", "B", "C")
                subdeckDisplayNames(PreviewSamples.ankiTable) shouldBe listOf("명사", "동사")
            }

            test("공통 상위 아래에 단계가 더 있으면 남은 경로를 모두 보인다") {
                val nested = subdeckTable.copy(subdecks = subdecks("Lang::JP::단어::N5", "Lang::JP::문법"))

                subdeckDisplayNames(nested) shouldBe listOf("단어::N5", "문법")
            }

            test("상위 덱과 그 하위 덱이 함께 있거나 덱이 하나면 경로를 그대로 보인다") {
                subdeckDisplayNames(subdeckTable.copy(subdecks = subdecks("JLPT N5", "JLPT N5::단어"))) shouldBe
                    listOf("JLPT N5", "JLPT N5::단어")
                subdeckDisplayNames(subdeckTable.copy(subdecks = subdecks("JLPT N5"))) shouldBe listOf("JLPT N5")
            }

            test("전체 선택 상태는 모두 고르면 켜짐, 일부면 일부, 없으면 꺼짐이다") {
                subdeckSelectAllState(subdeckTable, setOf("a", "b", "c")) shouldBe ToggleableState.On
                subdeckSelectAllState(subdeckTable, setOf("a")) shouldBe ToggleableState.Indeterminate
                subdeckSelectAllState(subdeckTable, emptySet()) shouldBe ToggleableState.Off
            }

            test("전체 선택을 누르면 모두 골랐을 때만 모두 풀고 그 밖에는 모두 고른다") {
                toggledAllSubdecks(subdeckTable, persistentSetOf("a", "b", "c")) shouldBe emptySet()
                toggledAllSubdecks(subdeckTable, persistentSetOf("b")) shouldBe setOf("a", "b", "c")
                toggledAllSubdecks(subdeckTable, persistentSetOf()) shouldBe setOf("a", "b", "c")
            }

            test("고른 장수가 상한을 넘으면 상한 초과 사유다") {
                reasonOf(SettingsKind.Subdeck, subdeckTable, ImportSelection(subdeckIds = persistentSetOf("a", "b")), maxCards = 4) shouldBe
                    SettingsReason.SubdeckOverLimit
                reasonOf(SettingsKind.Subdeck, subdeckTable, ImportSelection(subdeckIds = persistentSetOf("a", "b")), maxCards = 5) shouldBe
                    null
            }

            test("고른 하위 덱이 모두 Image Occlusion이면 카드 0장 사유다") {
                reasonOf(SettingsKind.Subdeck, subdeckTable, ImportSelection(subdeckIds = persistentSetOf("c"))) shouldBe
                    SettingsReason.NoCards
            }

            test("하나도 고르지 않으면 사유 없이 둔다") {
                reasonOf(SettingsKind.Subdeck, subdeckTable, ImportSelection()) shouldBe null
            }
        }

        context("시트") {
            val maxCards = 3

            test("시트 줄 표시는 값 있는 행이 없으면 비어 있음, 제목 행을 더한 상한을 넘으면 넘음이다") {
                sheetRowLabel(ParsedSheet(index = 0, name = "a", rowCount = 0), maxCards) shouldBe SheetRowLabel.Empty
                sheetRowLabel(ParsedSheet(index = 0, name = "a", rowCount = 4), maxCards) shouldBe SheetRowLabel.Rows(4)
                sheetRowLabel(ParsedSheet(index = 0, name = "a", rowCount = 5), maxCards) shouldBe SheetRowLabel.OverLimit
            }

            test("빈 시트를 고르면 빈 시트 사유다") {
                val table = ImportTestTables.xlsx(sheetRows = listOf(3, 0))

                reasonOf(SettingsKind.Sheet, table, ImportSelection(sheetIndex = 1), maxCards) shouldBe SettingsReason.SheetEmpty
            }

            test("요약 행 수가 상한을 넘는 시트를 고르면 시트 상한 사유다") {
                val table = ImportTestTables.xlsx(sheetRows = listOf(3, 5))

                reasonOf(SettingsKind.Sheet, table, ImportSelection(sheetIndex = 1), maxCards) shouldBe SettingsReason.SheetOverLimit
            }

            test("읽은 시트의 데이터 행이 상한을 넘으면 요약 행 수가 후보여도 시트 상한 사유다") {
                val table = ImportTestTables.xlsx(sheetRows = listOf(4, 2), hasHeaderRow = false)

                reasonOf(SettingsKind.Sheet, table, ImportSelection(sheetIndex = 0), maxCards) shouldBe SettingsReason.SheetOverLimit
            }

            test("읽은 행 수가 상한 안이어도 읽기가 상한에서 멈췄으면 시트 상한 사유다") {
                val table = ImportTestTables.xlsx(sheetRows = listOf(3, 2)).copy(rowLimitReached = true)

                reasonOf(SettingsKind.Sheet, table, ImportSelection(sheetIndex = 0), maxCards) shouldBe SettingsReason.SheetOverLimit
            }

            test("상한 안의 데이터가 있는 시트는 사유가 없다") {
                reasonOf(SettingsKind.Sheet, ImportTestTables.xlsx(sheetRows = listOf(3, 2)), ImportSelection(), maxCards) shouldBe null
            }
        }

        context("카드 구성") {
            test("가져올 수 있는 유형을 모두 끄면 유형 없음 사유다") {
                val table = ImportTestTables.apkg("modern_anki21b_main.apkg", "default")
                val allOff = defaultFieldMappings(table).mapValues { (_, mapping) -> mapping.copy(excluded = true) }

                draftSettingsReason(SettingsKind.CardLayout, table, ImportSelection(), allOff, maxCards = 10_000) shouldBe
                    SettingsReason.NoTypeEnabled
                draftSettingsReason(
                    SettingsKind.CardLayout,
                    table,
                    ImportSelection(),
                    defaultFieldMappings(table),
                    maxCards = 10_000,
                ) shouldBe
                    null
            }

            test("구분 기준은 카드를 만들어 봐야 알므로 미리 사유를 두지 않는다") {
                val table = ImportTestTables.text(rows = 5)

                reasonOf(SettingsKind.Delimiter, table, ImportSelection(), maxCards = 3) shouldBe null
            }
        }

        context("적용 뒤 판정") {
            test("상한을 넘으면 카드가 0장이어도 설정 종류별 상한 사유가 먼저다") {
                val over = PreviewSamples.preview(emptyList()).copy(limitExceeded = true)

                appliedPreviewReason(SettingsKind.Subdeck, over) shouldBe SettingsReason.SubdeckOverLimit
                appliedPreviewReason(SettingsKind.Sheet, over) shouldBe SettingsReason.SheetOverLimit
                appliedPreviewReason(SettingsKind.Delimiter, over) shouldBe SettingsReason.TooManyCards
                appliedPreviewReason(SettingsKind.CardLayout, over) shouldBe SettingsReason.TooManyCards
            }

            test("카드가 0장이면 카드 0장 사유이고 카드가 있으면 사유가 없다") {
                appliedPreviewReason(SettingsKind.CardLayout, PreviewSamples.preview(emptyList())) shouldBe SettingsReason.NoCards
                appliedPreviewReason(SettingsKind.CardLayout, PreviewSamples.textPreview) shouldBe null
            }
        }

        context("다시 읽은 표의 카드 구성") {
            test("항목 이름이 같은 유형은 고친 카드 구성을 이어 쓴다") {
                val before = ImportTestTables.xlsx(sheetRows = listOf(3, 2))
                val after = ImportTestTables.xlsx(sheetRows = listOf(3, 2), selectedSheet = 1)
                val edited = swapped(before)

                carryOverMappings(before, edited, after) shouldBe edited
            }

            test("항목 이름이 바뀐 유형은 기본 카드 구성으로 돌린다") {
                val before = ImportTestTables.text()
                val after = ImportTestTables.text(columns = 3)

                carryOverMappings(before, swapped(before), after) shouldBe defaultFieldMappings(after)
            }

            test("다시 읽은 표에 없는 유형의 카드 구성은 버린다") {
                val anki = ImportTestTables.apkg("modern_anki21b_main.apkg", "default")
                val onlyBasic = anki.copy(noteTypes = anki.noteTypes.take(1))

                carryOverMappings(anki, defaultFieldMappings(anki), onlyBasic).keys shouldBe setOf(anki.noteTypes.first().id)
            }
        }

        context("첫 카드 미리보기") {
            test("가져오는 유형마다 그 유형의 첫 카드를 만든다") {
                val table = ImportTestTables.text()

                firstPreviewCards(table, defaultFieldMappings(table), EMPTY_SIDE).values.map { card -> card.front } shouldBe listOf("열 1-1")
            }

            test("가져오기를 끈 유형은 첫 카드가 없다") {
                val table = ImportTestTables.text()
                val off = defaultFieldMappings(table).mapValues { (_, mapping) -> mapping.copy(excluded = true) }

                firstPreviewCards(table, off, EMPTY_SIDE).shouldBeEmpty()
            }
        }
    }

    private fun reasonOf(
        kind: SettingsKind,
        table: ParsedTable,
        selection: ImportSelection,
        maxCards: Int = 10_000,
    ): SettingsReason? = draftSettingsReason(kind, table, selection, defaultFieldMappings(table), maxCards)

    private fun subdecks(vararg paths: String): List<ParsedSubdeck> =
        paths.mapIndexed { index, path -> ParsedSubdeck(id = "deck$index", path = path, noteCount = 1) }

    private fun swapped(table: ParsedTable): Map<String, FieldMapping> =
        defaultFieldMappings(table).mapValues { (_, mapping) ->
            mapping.copy(targets = mapOf(FieldMapping.FRONT to mapping.backFields, FieldMapping.BACK to mapping.frontFields))
        }

    private companion object {
        const val EMPTY_SIDE = "(비어 있음)"
    }
}
