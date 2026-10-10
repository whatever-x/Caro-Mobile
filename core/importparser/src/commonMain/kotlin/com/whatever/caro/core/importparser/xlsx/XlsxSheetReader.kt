package com.whatever.caro.core.importparser.xlsx

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

        private fun closeCell(state: CellState) {
            finishCell(state)?.let { value -> row?.put(state.column, value) }
            nextColumn = state.column + 1
            cell = null
        }
    }

    fun readRows(
        archive: ZipArchive,
        part: String,
        maxRows: Int,
    ): List<XlsxRow> {
        val collector = RowCollector()
        archive.forEachEvent(part) { event ->
            if (collector.rows.size >= maxRows) return collector.rows
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

    private fun finishCell(state: CellState): XlsxCell? {
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

        /** `AB12` 같은 셀 주소의 열 문자를 0부터 시작하는 번호로 바꾼다. */
        fun columnIndex(reference: String): Int? {
            var column = 0
            var letters = 0
            for (char in reference) {
                if (char !in 'A'..'Z') break
                column = column * ALPHABET_SIZE + (char - 'A' + 1)
                letters++
            }
            return if (letters == 0) null else column - 1
        }

        fun countRows(
            archive: ZipArchive,
            part: String,
        ): Int {
            var count = 0
            archive.forEachEvent(part) { event -> if (event is XmlEvent.Start && event.name == "row") count++ }
            return count
        }
    }
}
