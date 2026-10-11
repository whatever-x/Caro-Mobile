package com.whatever.caro.feature.importer.step

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable

/** 하위 덱 선택과 시트 선택 화면 테스트가 쓰는 요약 읽기 결과다. 선택 단계는 행이 아니라 요약만 보므로 rows는 비운다. */
internal object SelectionStepTables {
    const val APKG_NAME = "JLPT N5 단어.apkg"
    const val XLSX_NAME = "JLPT N5 단어장.xlsx"

    private const val PARENT = "JLPT N5"

    /** 하위 덱 id는 1부터 순서대로 붙는다. 경로는 모두 같은 상위 덱 아래에 있다. */
    fun apkg(
        subdecks: List<Pair<String, Int>> = listOf("단어" to 1_204, "문법" to 310),
        noDeckCount: Int = 3,
    ): ParsedTable {
        val parsedSubdecks =
            subdecks.mapIndexed { index, (name, noteCount) ->
                ParsedSubdeck(id = (index + 1).toString(), path = "$PARENT::$name", noteCount = noteCount)
            }
        val totalNotes = parsedSubdecks.sumOf { subdeck -> subdeck.noteCount } + noDeckCount
        return ParsedTable(
            format = ImportFormat.APKG,
            sourceName = APKG_NAME,
            htmlMode = HtmlMode.HTML,
            subdecks = parsedSubdecks,
            noteTypes =
                listOf(
                    ParsedNoteType(
                        id = "basic",
                        name = "기본",
                        fieldNames = listOf("앞면", "뒷면"),
                        isCloze = false,
                        noteCount = totalNotes,
                    ),
                ),
            rows = emptyList(),
        )
    }

    /** 시트 index는 0부터 순서대로 붙는다. */
    fun xlsx(
        sheets: List<Pair<String, Int>> =
            listOf("N5 단어" to 820, "N4 단어" to 640, "메모" to 0),
        hasHeaderRow: Boolean = true,
    ): ParsedTable {
        val parsedSheets =
            sheets.mapIndexed { index, (name, rowCount) -> ParsedSheet(index = index, name = name, rowCount = rowCount) }
        return ParsedTable(
            format = ImportFormat.XLSX,
            sourceName = XLSX_NAME,
            htmlMode = HtmlMode.PLAIN,
            sheets = parsedSheets,
            noteTypes =
                listOf(
                    ParsedNoteType(
                        id = "sheet",
                        name = parsedSheets.first().name,
                        fieldNames = listOf("열 1", "열 2"),
                        isCloze = false,
                        noteCount = parsedSheets.first().rowCount,
                    ),
                ),
            rows = emptyList(),
            layout = ParsedLayout(delimiter = null, hasHeaderRow = hasHeaderRow),
        )
    }
}
