package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import kotlinx.collections.immutable.persistentSetOf

internal object ImportSelectionScreenshotScenario {
    const val SUBDECK_OVER_LIMIT_ID = "importer.subdeck.over_limit"
    const val SHEET_SELECT_ID = "importer.sheet.select"
    const val TEXT_CHECK_DEFAULT_ID = "importer.text_check.default"

    private const val TEXT_NOTE_TYPE_ID = "text"
    private val TEXT_FIELD_NAMES = listOf("열 1", "열 2", "열 3", "열 4")

    private val TEXT_ROWS =
        listOf(
            listOf("学校", "학교", "がっこう", "学校に行きます。"),
            listOf("先生", "선생님", "せんせい", "先生に聞きます。"),
            listOf("読む", "읽다", "よむ", "本を読みます。"),
            listOf("毎日", "매일", "まいにち", "毎日勉強します。"),
            listOf("友達", "친구", "ともだち", "友達と遊びます。"),
            listOf("水", "물", "みず", "水を飲みます。"),
            listOf("山", "산", "やま", "山に登ります。"),
            listOf("川", "강", "かわ", "川で泳ぎます。"),
            listOf("花", "꽃", "はな", "花を買います。"),
            listOf("犬", "개", "いぬ", "犬と歩きます。"),
        )

    // 하위 덱 합계 12,400장에 덱 없음 3개를 더해 노트 유형 합계를 맞춘다.
    private val OVER_LIMIT_TABLE =
        ParsedTable(
            format = ImportFormat.APKG,
            sourceName = "JLPT N5 단어.apkg",
            htmlMode = HtmlMode.HTML,
            subdecks =
                listOf(
                    ParsedSubdeck(id = "1", path = "JLPT N5::단어", noteCount = 9_800),
                    ParsedSubdeck(id = "2", path = "JLPT N5::문법", noteCount = 2_600),
                ),
            noteTypes =
                listOf(
                    ParsedNoteType(
                        id = "basic",
                        name = "기본",
                        fieldNames = listOf("앞면", "뒷면"),
                        isCloze = false,
                        noteCount = 12_403,
                    ),
                ),
            rows = emptyList(),
        )

    private val SHEET_TABLE =
        ParsedTable(
            format = ImportFormat.XLSX,
            sourceName = "JLPT N5 단어장.xlsx",
            htmlMode = HtmlMode.PLAIN,
            sheets =
                listOf(
                    ParsedSheet(index = 0, name = "N5 단어", rowCount = 820),
                    ParsedSheet(index = 1, name = "N4 단어", rowCount = 640),
                    ParsedSheet(index = 2, name = "메모", rowCount = 0),
                ),
            noteTypes =
                listOf(
                    ParsedNoteType(
                        id = "sheet",
                        name = "N5 단어",
                        fieldNames = listOf("열 1", "열 2"),
                        isCloze = false,
                        noteCount = 820,
                    ),
                ),
            rows = emptyList(),
            layout = ParsedLayout(delimiter = null, hasHeaderRow = true),
        )

    private val TEXT_TABLE =
        ParsedTable(
            format = ImportFormat.TEXT,
            sourceName = "words.txt",
            htmlMode = HtmlMode.PLAIN,
            noteTypes =
                listOf(
                    ParsedNoteType(
                        id = TEXT_NOTE_TYPE_ID,
                        name = "words.txt",
                        fieldNames = TEXT_FIELD_NAMES,
                        isCloze = false,
                        noteCount = TEXT_ROWS.size,
                    ),
                ),
            rows =
                TEXT_ROWS.map { values ->
                    ParsedRow(noteTypeId = TEXT_NOTE_TYPE_ID, fields = TEXT_FIELD_NAMES.zip(values).toMap())
                },
            layout = ParsedLayout(delimiter = "\t", hasHeaderRow = false),
        )

    @Composable
    fun RenderSubdeckOverLimit() {
        ImportScreenshotScenario.Render(
            ImportDeckState(
                step = ImportStep.SubdeckSelect,
                table = OVER_LIMIT_TABLE,
                selection = ImportSelection(subdeckIds = persistentSetOf("1", "2")),
            ),
        )
    }

    @Composable
    fun RenderSheetSelect() {
        ImportScreenshotScenario.Render(
            ImportDeckState(
                step = ImportStep.SheetSelect,
                table = SHEET_TABLE,
                selection = ImportSelection(sheetIndex = 0, hasHeaderRow = true),
            ),
        )
    }

    @Composable
    fun RenderTextCheckDefault() {
        ImportScreenshotScenario.Render(ImportDeckState(step = ImportStep.TextCheck, table = TEXT_TABLE))
    }
}
