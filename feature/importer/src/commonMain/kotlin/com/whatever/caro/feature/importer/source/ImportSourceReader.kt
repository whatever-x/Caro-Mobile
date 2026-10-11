package com.whatever.caro.feature.importer.source

import com.whatever.caro.core.importparser.ImportParser
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ParsedTable
import okio.Path

/** ImportParser가 final 클래스라 ViewModel 테스트가 대본대로 읽는 가짜로 바꿀 수 있게 두는 경계. 선택 값이 null이면 파서의 자동 판별을 따른다. */
internal interface ImportSourceReader {
    /** 파일 IO가 블로킹이므로 io 디스패처에서 부른다. [sourceName]은 사용자에게 보인 파일 이름이다. */
    suspend fun readFile(
        path: Path,
        sourceName: String,
        dateStyle: SpreadsheetDateStyle,
        subdeckIds: Set<String>? = null,
        sheetIndex: Int = 0,
        delimiter: Char? = null,
        hasHeaderRow: Boolean? = null,
    ): ParsedTable

    suspend fun readText(
        text: String,
        sourceName: String,
        dateStyle: SpreadsheetDateStyle,
        delimiter: Char? = null,
        hasHeaderRow: Boolean? = null,
    ): ParsedTable
}

internal class ParserImportSourceReader(
    private val parser: ImportParser,
    private val limits: ImportLimitsConfig,
) : ImportSourceReader {
    override suspend fun readFile(
        path: Path,
        sourceName: String,
        dateStyle: SpreadsheetDateStyle,
        subdeckIds: Set<String>?,
        sheetIndex: Int,
        delimiter: Char?,
        hasHeaderRow: Boolean?,
    ): ParsedTable =
        parser.parseFile(
            path = path,
            sourceName = sourceName,
            options = buildParseOptions(limits, dateStyle, subdeckIds, sheetIndex, delimiter, hasHeaderRow),
        )

    override suspend fun readText(
        text: String,
        sourceName: String,
        dateStyle: SpreadsheetDateStyle,
        delimiter: Char?,
        hasHeaderRow: Boolean?,
    ): ParsedTable =
        parser.parseText(
            text = text,
            sourceName = sourceName,
            options = buildParseOptions(limits, dateStyle, delimiter = delimiter, hasHeaderRow = hasHeaderRow),
        )
}
