package com.whatever.caro.core.importparser.xlsx

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.archive.ZipArchive

internal sealed interface XlsxCell {
    data class Value(
        val text: String,
        val rawNumberFormat: Boolean = false,
        val formulaWithoutValue: Boolean = false,
    ) : XlsxCell

    /** 공유 문자열은 시트를 다 읽은 뒤 필요한 번호만 보관하려고 번호로 둔다. */
    data class Shared(
        val index: Int,
    ) : XlsxCell
}

/** 열 번호(0부터)별 셀. 값이 없는 칸과 병합 영역의 나머지 칸은 들어 있지 않다. */
internal typealias XlsxRow = Map<Int, XlsxCell>

internal class XlsxSheetReader(
    private val cellFormatIds: List<Int>,
    private val numberFormat: XlsxNumberFormat,
    private val maxColumns: Int,
    private val maxCells: Int,
) {
    private class CellState(
        val column: Int,
        val type: String?,
        val style: Int,
    ) {
        var value: StringBuilder? = null
        var inline: StringBuilder? = null
        var hasFormula = false
        var inPhonetic = false
        var capture: StringBuilder? = null
    }

    private inner class RowCollector {
        val rows = mutableListOf<XlsxRow>()
        private var row: MutableMap<Int, XlsxCell>? = null
        private var cell: CellState? = null
        private var nextColumn = 0
        private var cellCount = 0

        fun accept(event: XmlEvent) {
            when (event) {
                is XmlEvent.Start -> start(event)
                is XmlEvent.Text -> cell?.capture?.append(event.text)
                is XmlEvent.End -> end(event.name)
            }
        }

        private fun start(event: XmlEvent.Start) {
            when (event.name) {
                "row" -> {
                    row = LinkedHashMap()
                    nextColumn = 0
                }

                "c" -> {
                    cell = startCell(event, nextColumn)
                }

                else -> {
                    cell?.let { state -> onCellChildStart(state, event.name) }
                }
            }
        }

        private fun end(name: String) {
            when (name) {
                "c" -> cell?.let(::closeCell)
                "row" -> row?.takeIf { it.hasValue() }?.let { rows += it }
                else -> cell?.let { state -> onCellChildEnd(state, name) }
            }
        }

        // 열과 칸 수는 셀을 닫는 즉시 확인해 상한을 넘는 시트의 나머지를 읽거나 쌓지 않는다.
        private fun closeCell(state: CellState) {
            finishCell(state)?.let { value ->
                if (state.column >=
                    maxColumns
                ) {
                    throw ImportParseException(ImportParseErrorKind.TOO_MANY_COLUMNS, "column ${state.column + 1}")
                }
                if (++cellCount > maxCells) throw ImportParseException(ImportParseErrorKind.CONTENT_TOO_LONG, "sheet over $maxCells cells")
                row?.put(state.column, value)
            }
            nextColumn = state.column + 1
            cell = null
        }
    }

    fun readRows(
        archive: ZipArchive,
        part: String,
        limit: Int,
    ): List<XlsxRow> {
        val collector = RowCollector()
        archive.forEachEvent(part) { event ->
            if (collector.rows.size >= limit) return collector.rows
            collector.accept(event)
        }
        return collector.rows
    }

    private fun startCell(
        event: XmlEvent.Start,
        fallbackColumn: Int,
    ) = CellState(
        column = event.attributes["r"]?.let(::columnIndex) ?: fallbackColumn,
        type = event.attributes["t"],
        style = event.attributes["s"]?.toIntOrNull() ?: 0,
    )

    private fun onCellChildStart(
        state: CellState,
        name: String,
    ) {
        when (name) {
            "v" -> state.capture = StringBuilder().also { state.value = it }
            "f" -> state.hasFormula = true
            "is" -> state.inline = StringBuilder()
            "rPh" -> state.inPhonetic = true
            "t" -> if (!state.inPhonetic) state.capture = state.inline
        }
    }

    private fun onCellChildEnd(
        state: CellState,
        name: String,
    ) {
        when (name) {
            "v", "t" -> state.capture = null
            "rPh" -> state.inPhonetic = false
        }
    }

    /** 값이 빈 셀은 열 수와 메모리에 넣지 않는다. 캐시 값이 없는 수식은 경고 수를 세려고 남긴다. */
    private fun finishCell(state: CellState): XlsxCell? =
        rawCell(state)?.takeUnless {
            it is XlsxCell.Value && it.text.isEmpty() &&
                !it.formulaWithoutValue
        }

    private fun rawCell(state: CellState): XlsxCell? {
        val stored = state.value?.toString()
        val missingFormulaValue = XlsxCell.Value("", formulaWithoutValue = true)
        return when (state.type) {
            "s" -> {
                stored?.trim()?.toIntOrNull()?.let { index -> XlsxCell.Shared(index) }
            }

            "inlineStr" -> {
                XlsxCell.Value(state.inline?.toString().orEmpty())
            }

            "str", "e" -> {
                stored?.let { XlsxCell.Value(it) } ?: missingFormulaValue.takeIf { state.hasFormula }
            }

            "b" -> {
                stored?.let { XlsxCell.Value(if (it.trim() == "1") "TRUE" else "FALSE") }
                    ?: missingFormulaValue.takeIf { state.hasFormula }
            }

            else -> {
                stored?.let { number(it, state.style) } ?: missingFormulaValue.takeIf { state.hasFormula }
            }
        }
    }

    private fun number(
        stored: String,
        style: Int,
    ): XlsxCell.Value {
        val formatted = numberFormat.format(stored.trim(), cellFormatIds.getOrElse(style) { 0 })
        return XlsxCell.Value(formatted.text, rawNumberFormat = formatted.raw)
    }

    private fun XlsxRow.hasValue(): Boolean = values.any { cell -> cell !is XlsxCell.Value || cell.text.isNotEmpty() }

    companion object {
        private const val ALPHABET_SIZE = 26

        // Excel 열은 XFD(16,384번째)까지다. 그보다 긴 주소는 정상 파일에 없고 그대로 믿으면 열 목록이 수만 칸으로 늘어난다.
        private const val MAX_COLUMN_LETTERS = 3
        private const val MAX_COLUMN_INDEX = 16_383

        /** `AB12` 같은 셀 주소의 열 문자를 0부터 시작하는 번호로 바꾼다. 범위 밖이면 null이라 순서대로 센 열을 쓴다. */
        fun columnIndex(reference: String): Int? {
            val letters = reference.takeWhile { char -> char in 'A'..'Z' }
            if (letters.isEmpty() || letters.length > MAX_COLUMN_LETTERS) return null
            val column = letters.fold(0) { value, char -> value * ALPHABET_SIZE + (char - 'A' + 1) } - 1
            return column.takeIf { it <= MAX_COLUMN_INDEX }
        }

        /** 시트 목록 표시용이라 [limit]에서 세기를 멈춘다. 그 이상은 어차피 상한을 넘는 시트다. */
        fun countRows(
            archive: ZipArchive,
            part: String,
            limit: Int,
        ): Int {
            var count = 0
            archive.forEachEvent(part) { event ->
                if (event is XmlEvent.Start && event.name == "row") count++
                if (count >= limit) return limit
            }
            return count
        }
    }
}
