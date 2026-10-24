package com.whatever.caro.core.importparser.text

import com.whatever.caro.core.importparser.HtmlSignal
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.ParseOptions
import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.importparser.model.countWarning
import com.whatever.caro.core.importparser.table.ColumnNames
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class TextTableReader(
    private val options: ParseOptions,
) {
    private class Scan(
        val titles: List<String>?,
        val rows: List<List<String>>,
        val hasHeaderRow: Boolean,
        val columnsFromHeader: Boolean,
        val htmlSignal: Boolean,
    )

    suspend fun read(
        chunks: Iterator<String>,
        sourceName: String,
    ): ParsedTable {
        val prefix = takePrefix(chunks)
        val (header, bodyStart) = AnkiTextHeader.parse(prefix)
        val body = prefix.substring(bodyStart)
        val delimiter = options.delimiter ?: header.separator ?: DelimiterDetector.detect(body) ?: DEFAULT_DELIMITER
        val cursor = CharCursor((sequenceOf(body) + chunks.asSequence()).iterator())
        val reader =
            DelimitedRecordReader(
                cursor = cursor,
                delimiter = delimiter,
                maxFields = options.maxColumns + header.metaColumns.size,
                maxFieldChars = options.maxFieldChars,
            )
        val scan = scan(reader, header, delimiter)
        val htmlMode =
            when (header.html ?: scan.htmlSignal) {
                true -> HtmlMode.HTML
                false -> HtmlMode.PLAIN
            }
        return buildTable(scan, sourceName, htmlMode, ParsedLayout(delimiter.toString(), scan.hasHeaderRow, scan.columnsFromHeader))
    }

    private suspend fun scan(
        reader: DelimitedRecordReader,
        header: AnkiTextHeader,
        delimiter: Char,
    ): Scan {
        var htmlSignal = false
        var cells = 0
        var chars = 0L

        fun take(): List<String>? =
            reader.nextRecord()?.let { raw ->
                if (!htmlSignal) htmlSignal = raw.any(HtmlSignal::containsSignal)
                raw.filterIndexed { index, _ -> index !in header.metaColumns }.also { values ->
                    cells += values.count { it.isNotEmpty() }
                    if (cells > options.maxCells) throw contentTooLong("table over ${options.maxCells} cells")
                    chars += values.sumOf { it.length }
                    if (chars > options.maxTotalChars) throw contentTooLong("table over ${options.maxTotalChars} chars")
                }
            }

        val first = take() ?: return Scan(null, emptyList(), hasHeaderRow = false, columnsFromHeader = false, htmlSignal = false)
        val hasHeaderRow = options.hasHeaderRow ?: (header.columns == null && ColumnNames.looksLikeTitleRow(first))
        val titles =
            when {
                hasHeaderRow -> first
                header.columns != null -> columnsOf(header, delimiter)
                else -> null
            }
        val rows = if (hasHeaderRow) mutableListOf() else mutableListOf(first)
        while (rows.size < options.maxRows) {
            rows += take() ?: break
            if (rows.size % CANCEL_CHECK_INTERVAL == 0) currentCoroutineContext().ensureActive()
        }
        return Scan(titles, rows, hasHeaderRow, columnsFromHeader = !hasHeaderRow && header.columns != null, htmlSignal = htmlSignal)
    }

    private fun columnsOf(
        header: AnkiTextHeader,
        delimiter: Char,
    ): List<String> {
        val reader = DelimitedRecordReader(CharCursor(listOfNotNull(header.columns).iterator()), delimiter)
        return reader.nextRecord().orEmpty().filterIndexed { index, _ -> index !in header.metaColumns }
    }

    private fun buildTable(
        scan: Scan,
        sourceName: String,
        htmlMode: HtmlMode,
        layout: ParsedLayout,
    ): ParsedTable {
        val width = tableWidth(scan)
        if (width > options.maxColumns) throw ImportParseException(ImportParseErrorKind.TOO_MANY_COLUMNS, "$width columns")
        if (width.toLong() * scan.rows.size > options.maxCells) throw contentTooLong("table over ${options.maxCells} cells")
        val names = ColumnNames.fieldNames(scan.titles, width)
        val rows =
            scan.rows.map { values ->
                ParsedRow(
                    noteTypeId = NOTE_TYPE_ID,
                    fields = names.withIndex().associate { (index, name) -> name to values.getOrElse(index) { "" } },
                )
            }
        val noteTypes =
            if (width == 0) {
                emptyList()
            } else {
                listOf(ParsedNoteType(NOTE_TYPE_ID, sourceName, names, isCloze = false, noteCount = rows.size))
            }
        val padded = scan.rows.count { row -> row.size < width }
        return ParsedTable(
            format = ImportFormat.TEXT,
            sourceName = sourceName,
            htmlMode = htmlMode,
            noteTypes = noteTypes,
            rows = rows,
            warnings = listOfNotNull(countWarning(ParseWarningCode.TEXT_INCONSISTENT_COLUMNS, padded)),
            layout = layout,
            // 상한 행에서 읽기를 멈췄으므로 뒤에 행이 더 있을 수 있다. 제외 카드가 생겨도 상한 초과 판정을 유지한다.
            rowLimitReached = scan.rows.size >= options.maxRows,
        )
    }

    /**
     * 행 값 맵은 행마다 항목 수만큼 칸을 만든다. 원래 열 수로 칸 예산을 넘으면 모든 행에서 빈 끝 열을 빼고 다시 잰다.
     * 빈 끝 열에는 값이 없으므로 잃는 글자가 없다.
     */
    private fun tableWidth(scan: Scan): Int {
        val rawWidth = maxOf(scan.titles?.size ?: 0, scan.rows.maxOfOrNull { row -> row.size } ?: 0)
        if (rawWidth.toLong() * scan.rows.size <= options.maxCells) return rawWidth
        return (listOfNotNull(scan.titles) + scan.rows).maxOfOrNull { values -> values.indexOfLast { it.isNotEmpty() } + 1 } ?: 0
    }

    private fun contentTooLong(reason: String) = ImportParseException(ImportParseErrorKind.CONTENT_TOO_LONG, reason)

    private fun takePrefix(chunks: Iterator<String>): String {
        val prefix = StringBuilder()
        while (prefix.length < PREFIX_CHARS && chunks.hasNext()) prefix.append(chunks.next())
        return prefix.toString()
    }

    private companion object {
        const val NOTE_TYPE_ID = "text"
        const val DEFAULT_DELIMITER = '\t'
        const val PREFIX_CHARS = 64 * 1024
        const val CANCEL_CHECK_INTERVAL = 256
    }
}
