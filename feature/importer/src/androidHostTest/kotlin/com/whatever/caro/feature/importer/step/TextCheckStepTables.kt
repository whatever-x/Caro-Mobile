package com.whatever.caro.feature.importer.step

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedTable

/** 처음 4줄은 Figma 프레임의 표시 값이다. */
internal object TextCheckStepTables {
    const val SOURCE_NAME = "words.txt"

    private const val NOTE_TYPE_ID = "text"

    val GENERATED_NAMES = listOf("열 1", "열 2", "열 3", "열 4")
    val HEADER_NAMES = listOf("단어", "뜻", "읽기", "예문")

    /** 10줄이다. 5번째 줄부터는 화면에 보이지 않아야 한다. */
    val ROWS =
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

    fun text(
        fieldNames: List<String> = GENERATED_NAMES,
        rows: List<List<String>> = ROWS,
        rowCount: Int = rows.size,
        delimiter: String = "\t",
        hasHeaderRow: Boolean = false,
        columnsFromHeader: Boolean = false,
    ): ParsedTable =
        ParsedTable(
            format = ImportFormat.TEXT,
            sourceName = SOURCE_NAME,
            htmlMode = HtmlMode.PLAIN,
            noteTypes =
                listOf(
                    ParsedNoteType(
                        id = NOTE_TYPE_ID,
                        name = SOURCE_NAME,
                        fieldNames = fieldNames,
                        isCloze = false,
                        noteCount = rowCount,
                    ),
                ),
            rows = List(rowCount) { index -> rows[index % rows.size] }.map { values -> row(fieldNames, values) },
            layout = ParsedLayout(delimiter = delimiter, hasHeaderRow = hasHeaderRow, columnsFromHeader = columnsFromHeader),
        )

    /** 열 이름은 열 1부터 [columnCount]까지이고 셀 값은 `r행c열`이다. */
    fun wide(
        columnCount: Int,
        rowCount: Int = 10,
    ): ParsedTable {
        val names = List(columnCount) { index -> "열 ${index + 1}" }
        val rows = List(rowCount) { row -> List(columnCount) { column -> "r${row + 1}c${column + 1}" } }
        return text(fieldNames = names, rows = rows)
    }

    private fun row(
        fieldNames: List<String>,
        values: List<String>,
    ): ParsedRow = ParsedRow(noteTypeId = NOTE_TYPE_ID, fields = fieldNames.zip(values).toMap())
}
