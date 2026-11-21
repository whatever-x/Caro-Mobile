package com.whatever.caro.feature.importer.source

import com.whatever.caro.core.importparser.ImportSourceName
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ParsedTable
import kotlinx.coroutines.withContext
import okio.Path

/** 다시 읽기가 첫 읽기와 같은 원본, 파일 이름, 날짜 형식을 쓰도록 읽기 인자를 한곳에 묶는다. */
internal sealed interface ImportReadSource {
    val name: String
    val dateStyle: SpreadsheetDateStyle

    /** 소문자 확장자. 붙여넣기는 파일이 아니므로 빈 문자열이다. */
    val extension: String

    suspend fun read(
        reader: ImportSourceReader,
        dispatchers: ImportDispatchers,
        subdeckIds: Set<String>? = null,
        sheetIndex: Int = 0,
        delimiter: Char? = null,
        hasHeaderRow: Boolean? = null,
    ): ParsedTable

    /** 임시 사본 경로에는 확장자가 없으므로 [name]은 사용자에게 보인 파일 이름이다. */
    class File(
        val path: Path,
        override val name: String,
        override val dateStyle: SpreadsheetDateStyle,
    ) : ImportReadSource {
        override val extension: String = ImportSourceName.supportedExtension(name).orEmpty()

        override suspend fun read(
            reader: ImportSourceReader,
            dispatchers: ImportDispatchers,
            subdeckIds: Set<String>?,
            sheetIndex: Int,
            delimiter: Char?,
            hasHeaderRow: Boolean?,
        ): ParsedTable =
            withContext(dispatchers.io) {
                reader.readFile(path, name, dateStyle, subdeckIds, sheetIndex, delimiter, hasHeaderRow)
            }
    }

    class Paste(
        val text: String,
        override val name: String,
        override val dateStyle: SpreadsheetDateStyle,
    ) : ImportReadSource {
        override val extension: String = ""

        override suspend fun read(
            reader: ImportSourceReader,
            dispatchers: ImportDispatchers,
            subdeckIds: Set<String>?,
            sheetIndex: Int,
            delimiter: Char?,
            hasHeaderRow: Boolean?,
        ): ParsedTable =
            withContext(dispatchers.default) {
                reader.readText(text, name, dateStyle, delimiter, hasHeaderRow)
            }
    }
}
