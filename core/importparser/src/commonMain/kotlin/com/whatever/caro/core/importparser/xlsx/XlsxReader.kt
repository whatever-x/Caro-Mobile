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

/** 엑셀 통합 문서에서 시트 하나를 한 노트 유형의 표로 읽는다. 다른 시트는 행 수만 센다. */
internal class XlsxReader(
    private val archive: ZipArchive,
    private val options: ParseOptions,
) {
    suspend fun read(sourceName: String): ParsedTable {
        val workbook = XlsxWorkbook.read(archive)
        val sheets =
            workbook.sheets.mapIndexed { index, sheet ->
                currentCoroutineContext().ensureActive()
                ParsedSheet(index, sheet.name, sheet.part?.takeIf(archive::contains)?.let { XlsxSheetReader.countRows(archive, it) } ?: 0)
            }
        val selected =
            workbook.sheets.getOrNull(options.sheetIndex)
                ?: throw ImportParseException(ImportParseErrorKind.UNSUPPORTED_FILE, "sheet ${options.sheetIndex} not found")
        val values = selected.part?.takeIf(archive::contains)?.let { part -> readValues(workbook, part) } ?: SheetValues.EMPTY
        return buildTable(sourceName, selected.name, sheets, values)
    }

    private class SheetValues(
        val rows: List<List<String>>,
        val cells: List<List<XlsxCell.Value>>,
    ) {
        companion object {
            val EMPTY = SheetValues(emptyList(), emptyList())
        }
    }

    private suspend fun readValues(
        workbook: XlsxWorkbook,
        part: String,
    ): SheetValues {
        val numberFormat = XlsxNumberFormat(workbook.date1904, options.dateStyle)
        val sheetReader = XlsxSheetReader(readCellFormatIds(archive, workbook.stylesPart), numberFormat)
        // 제목 행이 있으면 데이터 행을 maxRows개 남기려고 한 행을 더 읽는다.
        val rawRows = sheetReader.readRows(archive, part, options.maxRows + 1)
        currentCoroutineContext().ensureActive()
        val needed = rawRows.flatMapTo(HashSet()) { row -> row.values.filterIsInstance<XlsxCell.Shared>().map { it.index } }
        val sharedStrings = readSharedStrings(archive, workbook.sharedStringsPart, needed)
        val cells =
            rawRows
                .map { row ->
                    val width = (row.keys.maxOrNull() ?: -1) + 1
                    List(width) { column -> resolve(row[column], sharedStrings) }
                }.filter { row -> row.any { cell -> cell.text.isNotEmpty() } }
        return SheetValues(cells.map { row -> row.map { it.text } }, cells)
    }

    private fun resolve(
        cell: XlsxCell?,
        sharedStrings: Map<Int, String>,
    ): XlsxCell.Value =
        when (cell) {
            null -> XlsxCell.Value("")
            is XlsxCell.Value -> cell
            is XlsxCell.Shared -> XlsxCell.Value(sharedStrings[cell.index].orEmpty())
        }

    private fun buildTable(
        sourceName: String,
        sheetName: String,
        sheets: List<ParsedSheet>,
        values: SheetValues,
    ): ParsedTable {
        val first = values.rows.firstOrNull()
        val hasHeaderRow = first != null && (options.hasHeaderRow ?: ColumnNames.looksLikeTitleRow(first))
        val dataStart = if (hasHeaderRow) 1 else 0
        val dataRows = values.rows.drop(dataStart).take(options.maxRows)
        val dataCells =
            values.cells
                .drop(dataStart)
                .take(options.maxRows)
                .flatten()
        val width = maxOf(if (hasHeaderRow) first.orEmpty().size else 0, dataRows.maxOfOrNull { it.size } ?: 0)
        val names = ColumnNames.fieldNames(if (hasHeaderRow) first else null, width)
        val rows =
            dataRows.map { row ->
                ParsedRow(
                    noteTypeId = NOTE_TYPE_ID,
                    fields =
                        names.withIndex().associate { (index, name) ->
                            name to
                                row.getOrElse(index) { "" }
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
                    countWarning(ParseWarningCode.XLSX_FORMULA_WITHOUT_VALUE, dataCells.count { it.formulaWithoutValue }),
                    countWarning(ParseWarningCode.XLSX_NUMBER_FORMAT_RAW, dataCells.count { it.rawNumberFormat }),
                ),
        )
    }

    private companion object {
        const val NOTE_TYPE_ID = "xlsx"
    }
}
