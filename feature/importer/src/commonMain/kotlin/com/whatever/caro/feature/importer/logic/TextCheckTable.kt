package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedTable

private const val MAX_SHOWN_COLUMNS = 20
private const val MAX_SHOWN_ROWS = 4
private const val DEFAULT_DELIMITER = '\t'
private val BuiltInDelimiters = listOf('\t', ',', ';')

// 파서가 제목 없는 열에 붙이는 이름이다. 같은 이름이 겹치면 뒤에 ` (2)`부터 번호가 붙는다.
private val GeneratedNamePattern = Regex("""열 (\d+)(?: \(\d+\))?""")

internal data class TextCheckInfo(
    val columnCount: Int,
    val shownColumnCount: Int,
    val rowCount: Int,
    val shownRowCount: Int,
)

/** [Numbered]는 이름이 없는 열이다. 번호만 두고 문구는 화면 언어로 만든다. */
internal sealed interface TextCheckHeader {
    data class Named(
        val text: String,
    ) : TextCheckHeader

    data class Numbered(
        val number: Int,
    ) : TextCheckHeader
}

internal data class TextCheckTable(
    val info: TextCheckInfo,
    val headers: List<TextCheckHeader>,
    val rows: List<List<String>>,
) {
    val hasHiddenColumns: Boolean
        get() = info.columnCount > info.shownColumnCount
}

internal fun buildTextCheckTable(table: ParsedTable): TextCheckTable {
    val fieldNames =
        table.noteTypes
            .firstOrNull()
            ?.fieldNames
            .orEmpty()
    val shownNames = fieldNames.take(MAX_SHOWN_COLUMNS)
    val shownRows =
        table.rows.take(MAX_SHOWN_ROWS).map { row ->
            shownNames.map { name -> truncateCell(row.fields[name].orEmpty()) }
        }
    val headers =
        shownNames.mapIndexed { index, name ->
            headerOf(name, number = index + 1, isDuplicated = fieldNames.count { other -> other == name } > 1)
        }
    return TextCheckTable(
        info =
            TextCheckInfo(
                columnCount = fieldNames.size,
                shownColumnCount = shownNames.size,
                rowCount = table.rows.size,
                shownRowCount = shownRows.size,
            ),
        headers = headers,
        rows = shownRows,
    )
}

/** 비었거나 겹치는 이름, 파서가 그 열 자리에 만든 이름은 사용자가 쓴 제목이 아니므로 번호로 돌려준다. */
private fun headerOf(
    name: String,
    number: Int,
    isDuplicated: Boolean,
): TextCheckHeader {
    val generatedNumber =
        GeneratedNamePattern
            .matchEntire(name)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    return if (name.isBlank() || isDuplicated || generatedNumber == number) {
        TextCheckHeader.Numbered(number)
    } else {
        TextCheckHeader.Named(truncateCell(name))
    }
}

internal enum class DelimiterKind {
    TAB,
    COMMA,
    SEMICOLON,
    PIPE,
    SPACE,
    COLON,
}

/** [kind]가 null이면 이름이 없는 문자라 [delimiter] 자체가 라벨이다. */
internal data class DelimiterChip(
    val delimiter: Char,
    val kind: DelimiterKind?,
    val isSelected: Boolean,
)

/** 선택된 칩이 하나도 없는 상태를 만들지 않도록 기본 구분자가 아니면 그 구분자 칩을 하나 더한다. */
internal fun delimiterChips(selected: Char): List<DelimiterChip> {
    val chips =
        BuiltInDelimiters.map { delimiter ->
            DelimiterChip(delimiter, delimiterKind(delimiter), isSelected = delimiter == selected)
        }
    return if (selected in BuiltInDelimiters) chips else chips + DelimiterChip(selected, delimiterKind(selected), isSelected = true)
}

private fun delimiterKind(delimiter: Char): DelimiterKind? =
    when (delimiter) {
        '\t' -> DelimiterKind.TAB
        ',' -> DelimiterKind.COMMA
        ';' -> DelimiterKind.SEMICOLON
        '|' -> DelimiterKind.PIPE
        ' ' -> DelimiterKind.SPACE
        ':' -> DelimiterKind.COLON
        else -> null
    }

internal fun resolveDelimiter(
    selected: Char?,
    layout: ParsedLayout?,
): Char = selected ?: layout?.delimiter?.firstOrNull() ?: DEFAULT_DELIMITER
