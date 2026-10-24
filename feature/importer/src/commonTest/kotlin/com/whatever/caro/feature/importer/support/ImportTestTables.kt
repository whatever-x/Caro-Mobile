package com.whatever.caro.feature.importer.support

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.ImportFixtures

internal object ImportTestTables {
    private const val XLSX_ID = "xlsx"
    private const val TEXT_ID = "text"
    private val FIELD_NAMES = listOf("열 1", "열 2")

    /** 요약 읽기가 돌려주는 모양이다. rows와 경고, 상한 표시를 비운다. */
    fun apkgSummary(
        fixture: String,
        variant: String = "all",
    ): ParsedTable = apkg(fixture, variant).copy(rows = emptyList(), rowLimitReached = false, warnings = emptyList())

    fun apkg(
        fixture: String,
        variant: String,
    ): ParsedTable = ImportFixtures.snapshot("apkg/$fixture", variant)

    /** 요약 표를 다시 읽은 결과로 쓸 수 있게 첫 노트 유형의 행 하나를 채운다. */
    fun withOneRow(table: ParsedTable): ParsedTable {
        val noteType = table.noteTypes.first()
        val row =
            ParsedRow(
                noteTypeId = noteType.id,
                subdeckId = table.subdecks.firstOrNull()?.id,
                fields = noteType.fieldNames.associateWith { name -> "$name 값" },
            )
        return table.copy(rows = listOf(row))
    }

    /** [sheetRows]는 시트별 행 수이고 [selectedSheet]의 행만 rows에 담긴다. 시트마다 노트 유형 이름이 달라 어느 시트를 읽었는지 구별된다. */
    fun xlsx(
        sourceName: String = "book.xlsx",
        sheetRows: List<Int> = listOf(3, 2, 0),
        selectedSheet: Int = 0,
        hasHeaderRow: Boolean = true,
    ): ParsedTable {
        val sheets = sheetRows.mapIndexed { index, rows -> ParsedSheet(index = index, name = "시트${index + 1}", rowCount = rows) }
        val selectedRows = sheetRows[selectedSheet]
        return ParsedTable(
            format = ImportFormat.XLSX,
            sourceName = sourceName,
            htmlMode = HtmlMode.PLAIN,
            sheets = sheets,
            noteTypes = listOf(noteType(XLSX_ID, sheets[selectedSheet].name, selectedRows)),
            rows = rows(XLSX_ID, selectedRows),
            layout = ParsedLayout(delimiter = null, hasHeaderRow = hasHeaderRow),
        )
    }

    fun text(
        sourceName: String = "words.txt",
        rows: Int = 3,
        delimiter: String = "\t",
        hasHeaderRow: Boolean = false,
    ): ParsedTable =
        ParsedTable(
            format = ImportFormat.TEXT,
            sourceName = sourceName,
            htmlMode = HtmlMode.PLAIN,
            noteTypes = listOf(noteType(TEXT_ID, sourceName, rows)),
            rows = rows(TEXT_ID, rows),
            layout = ParsedLayout(delimiter = delimiter, hasHeaderRow = hasHeaderRow),
        )

    /** 제목 줄만 있는 입력이다. 노트 유형은 있고 행이 0이라 텍스트 확인에서 제목 행을 고칠 수 있다. */
    fun titleOnlyText(sourceName: String = "붙여 넣은 카드"): ParsedTable = text(sourceName = sourceName, rows = 0, hasHeaderRow = true)

    /** 비어 있지 않은 줄이 하나도 없는 입력이다. */
    fun blankText(sourceName: String = "붙여 넣은 카드"): ParsedTable =
        ParsedTable(
            format = ImportFormat.TEXT,
            sourceName = sourceName,
            htmlMode = HtmlMode.PLAIN,
            noteTypes = emptyList(),
            rows = emptyList(),
            layout = ParsedLayout(delimiter = "\t", hasHeaderRow = false),
        )

    private fun noteType(
        id: String,
        name: String,
        noteCount: Int,
    ): ParsedNoteType = ParsedNoteType(id = id, name = name, fieldNames = FIELD_NAMES, isCloze = false, noteCount = noteCount)

    private fun rows(
        noteTypeId: String,
        count: Int,
    ): List<ParsedRow> =
        List(count) { index ->
            ParsedRow(noteTypeId = noteTypeId, fields = FIELD_NAMES.associateWith { name -> "$name-${index + 1}" })
        }
}
