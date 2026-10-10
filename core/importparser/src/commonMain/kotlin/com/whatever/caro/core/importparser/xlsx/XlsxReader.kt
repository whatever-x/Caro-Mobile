package com.whatever.caro.core.importparser.xlsx

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.ParseOptions
import com.whatever.caro.core.importparser.archive.ZipArchive
import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.importparser.model.countWarning
import com.whatever.caro.core.importparser.table.ColumnNames
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal class XlsxReader(
    private val archive: ZipArchive,
    private val options: ParseOptions,
) {
    private class SheetRow(
        val values: List<String>,
        val formulaWithoutValue: Int,
        val rawNumberFormat: Int,
    )

    /** [readLimitReached]는 빈 행을 걸러내기 전에 읽기를 멈췄는지 여부다. 멈췄으면 읽지 않은 행이 있을 수 있다. */
    private class SheetRows(
        val rows: List<SheetRow>,
        val readLimitReached: Boolean,
    )

    suspend fun read(sourceName: String): ParsedTable {
        val workbook = XlsxWorkbook.read(archive)
        // 제목 행 1개 + 데이터 상한 행까지만 세면 상한 초과 여부를 보일 수 있다.
        val countLimit = options.maxRows + 1
        val sheets =
            workbook.sheets.mapIndexed { index, sheet ->
                currentCoroutineContext().ensureActive()
                val rowCount = sheet.part?.takeIf(archive::contains)?.let { XlsxSheetReader.countRows(archive, it, countLimit) } ?: 0
                ParsedSheet(index, sheet.name, rowCount)
            }
        val selected =
            workbook.sheets.getOrNull(options.sheetIndex)
                ?: throw ImportParseException(ImportParseErrorKind.UNSUPPORTED_FILE, "sheet ${options.sheetIndex} not found")
        val rows =
            selected.part
                ?.takeIf(archive::contains)
                ?.let { part -> readRows(workbook, part) }
                ?: SheetRows(emptyList(), readLimitReached = false)
        return buildTable(sourceName, selected.name, sheets, rows)
    }

    private suspend fun readRows(
        workbook: XlsxWorkbook,
        part: String,
    ): SheetRows {
        val numberFormat = XlsxNumberFormat(workbook.date1904, options.dateStyle)
        val sheetReader =
            XlsxSheetReader(readCellFormatIds(archive, workbook.stylesPart), numberFormat, options.maxColumns, options.maxCells)
        // 제목 행이 있으면 데이터 행을 maxRows개 남기려고 한 행을 더 읽는다.
        val rawRows = sheetReader.readRows(archive, part, limit = options.maxRows + 1)
        currentCoroutineContext().ensureActive()
        val needed = rawRows.flatMapTo(HashSet()) { row -> row.values.filterIsInstance<XlsxCell.Shared>().map { it.index } }
        val sharedStrings = readSharedStrings(archive, workbook.sharedStringsPart, needed)
        val readLimit = options.maxRows + 1
        return SheetRows(rawRows.mapNotNull { row -> toSheetRow(row, sharedStrings) }, readLimitReached = rawRows.size >= readLimit)
    }

    /** 값이 있는 마지막 열까지만 칸을 만든다. 모든 칸이 비면 빈 행이라 건너뛴다. */
    private fun toSheetRow(
        row: XlsxRow,
        sharedStrings: Map<Int, String>,
    ): SheetRow? {
        val cells = row.mapValues { (_, cell) -> resolve(cell, sharedStrings) }
        val width =
            cells
                .filterValues { cell -> cell.text.isNotEmpty() }
                .keys
                .maxOrNull()
                ?.plus(1) ?: return null
        return SheetRow(
            values = List(width) { column -> cells[column]?.text.orEmpty() },
            formulaWithoutValue = cells.values.count { it.formulaWithoutValue },
            rawNumberFormat = cells.values.count { it.rawNumberFormat },
        )
    }

    private fun resolve(
        cell: XlsxCell,
        sharedStrings: Map<Int, String>,
    ): XlsxCell.Value =
        when (cell) {
            is XlsxCell.Value -> cell
            is XlsxCell.Shared -> XlsxCell.Value(sharedStrings[cell.index].orEmpty())
        }

    private fun buildTable(
        sourceName: String,
        sheetName: String,
        sheets: List<ParsedSheet>,
        sheetRows: SheetRows,
    ): ParsedTable {
        val first = sheetRows.rows.firstOrNull()?.values
        val hasHeaderRow = first != null && (options.hasHeaderRow ?: ColumnNames.looksLikeTitleRow(first))
        val dataRows = sheetRows.rows.drop(if (hasHeaderRow) 1 else 0)
        val kept = dataRows.take(options.maxRows)
        val width = maxOf(if (hasHeaderRow) first.orEmpty().size else 0, kept.maxOfOrNull { it.values.size } ?: 0)
        if (width.toLong() * kept.size > options.maxCells) {
            throw ImportParseException(ImportParseErrorKind.CONTENT_TOO_LONG, "table over ${options.maxCells} cells")
        }
        val names = ColumnNames.fieldNames(if (hasHeaderRow) first else null, width)
        val rows =
            kept.map { row ->
                ParsedRow(
                    noteTypeId = NOTE_TYPE_ID,
                    fields =
                        names.withIndex().associate { (index, name) ->
                            name to
                                row.values.getOrElse(index) { "" }
                        },
                )
            }
        val noteTypes =
            if (width ==
                0
            ) {
                emptyList()
            } else {
                listOf(ParsedNoteType(NOTE_TYPE_ID, sheetName, names, isCloze = false, noteCount = rows.size))
            }
        return ParsedTable(
            format = ImportFormat.XLSX,
            sourceName = sourceName,
            htmlMode = HtmlMode.PLAIN,
            sheets = sheets,
            noteTypes = noteTypes,
            rows = rows,
            warnings =
                listOfNotNull(
                    countWarning(ParseWarningCode.XLSX_FORMULA_WITHOUT_VALUE, kept.sumOf { it.formulaWithoutValue }),
                    countWarning(ParseWarningCode.XLSX_NUMBER_FORMAT_RAW, kept.sumOf { it.rawNumberFormat }),
                ),
            rowLimitReached = sheetRows.readLimitReached || dataRows.size >= options.maxRows,
        )
    }

    private companion object {
        const val NOTE_TYPE_ID = "xlsx"
    }
}
