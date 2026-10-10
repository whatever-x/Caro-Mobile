package com.whatever.caro.core.importparser.text

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.ParseOptions
import com.whatever.caro.core.importparser.model.ParseWarning
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.core.importparser.model.ParsedTable
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use

/** UTF-8로 읽다가 잘못된 바이트가 나오면 처음부터 CP949로 다시 읽는다. */
internal class TextFileReader(
    private val fileSystem: FileSystem,
    private val options: ParseOptions,
) {
    suspend fun read(
        path: Path,
        sourceName: String,
    ): ParsedTable =
        try {
            readWith(path, sourceName, ::decodeStrictUtf8)
        } catch (_: InvalidTextEncodingException) {
            readAsLegacyKorean(path, sourceName)
        }

    private suspend fun readAsLegacyKorean(
        path: Path,
        sourceName: String,
    ): ParsedTable {
        val decoder = options.legacyKoreanDecoder ?: throw encodingUndetermined("no CP949 decoder")
        val table =
            try {
                readWith(path, sourceName, decoder::decodeOrNull)
            } catch (_: InvalidTextEncodingException) {
                throw encodingUndetermined("invalid CP949 bytes")
            }
        return table.copy(warnings = listOf(ParseWarning(ParseWarningCode.ENCODING_FALLBACK_CP949)) + table.warnings)
    }

    private suspend fun readWith(
        path: Path,
        sourceName: String,
        decode: (ByteArray) -> String?,
    ): ParsedTable =
        fileSystem.source(path).buffer().use { source ->
            TextTableReader(options).read(EncodedTextChunks(source, decode), sourceName)
        }

    private fun encodingUndetermined(reason: String) = ImportParseException(ImportParseErrorKind.ENCODING_UNDETERMINED, reason)
}
