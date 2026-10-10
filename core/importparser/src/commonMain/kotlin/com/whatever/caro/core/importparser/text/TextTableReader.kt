package com.whatever.caro.core.importparser.text

import com.whatever.caro.core.importparser.HtmlSignal
import com.whatever.caro.core.importparser.ParseOptions
import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.importparser.model.ParsedTextLayout
import com.whatever.caro.core.importparser.model.countWarning
import com.whatever.caro.core.importparser.table.ColumnNames
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** 텍스트, CSV, 붙여넣기를 한 노트 유형의 표로 읽는다. */
internal class TextTableReader(
    private val options: ParseOptions,
) {
    private class Scan(
        val titles: List<String>?,
        val rows: List<List<String>>,
        val hasHeaderRow: Boolean,
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
        val scan = scan(DelimitedRecordReader(cursor, delimiter), header, delimiter)
        val htmlMode =
            when (header.html ?: scan.htmlSignal) {
                true -> HtmlMode.HTML
                false -> HtmlMode.PLAIN
            }
        return buildTable(scan, sourceName, htmlMode, ParsedTextLayout(delimiter.toString(), scan.hasHeaderRow))
    }

    private suspend fun scan(
        reader: DelimitedRecordReader,
        header: AnkiTextHeader,
        delimiter: Char,
    ): Scan {
        var htmlSignal = false

        fun take(): List<String>? =
            reader.nextRecord()?.let { raw ->
                if (!htmlSignal) htmlSignal = raw.any(HtmlSignal::containsSignal)
                raw.filterIndexed { index, _ -> index !in header.metaColumns }
            }

        val first = take() ?: return Scan(null, emptyList(), hasHeaderRow = false, htmlSignal = false)
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
        return Scan(titles, rows, hasHeaderRow, htmlSignal)
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
        layout: ParsedTextLayout,
    ): ParsedTable {
        val width = maxOf(scan.titles?.size ?: 0, scan.rows.maxOfOrNull { row -> row.size } ?: 0)
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
            textLayout = layout,
        )
    }

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
