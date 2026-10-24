package com.whatever.caro.core.importparser.detect

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.ParseOptions
import com.whatever.caro.core.importparser.archive.ZipArchive
import okio.ByteString.Companion.decodeHex
import okio.FileSystem
import okio.Path

internal sealed interface DetectedSource {
    data class Apkg(
        val archive: ZipArchive,
    ) : DetectedSource

    data class Xlsx(
        val archive: ZipArchive,
    ) : DetectedSource

    data object Text : DetectedSource
}

/** 지원하는 확장자만 받고, 확장자와 내용이 다르면 zip 내용을 따른다. */
internal object ImportFormatDetector {
    private val ZIP_SIGNATURE = "504b0304".decodeHex()
    private val OLE_SIGNATURE = "d0cf11e0".decodeHex()
    private val APKG_ENTRIES = listOf("meta", "collection.anki21b", "collection.anki21", "collection.anki2")
    private const val XLSX_WORKBOOK = "xl/workbook.xml"

    fun detect(
        fileSystem: FileSystem,
        path: Path,
        extension: String,
        fileBytes: Long,
        options: ParseOptions,
        checkCancelled: () -> Unit,
    ): DetectedSource {
        val head = fileSystem.read(path) { readByteString(minOf(fileBytes, ZIP_SIGNATURE.size.toLong())) }
        return when {
            head == ZIP_SIGNATURE -> detectZip(ZipArchive.open(fileSystem, path, fileBytes, options.archiveLimits, checkCancelled))

            head.startsWith(OLE_SIGNATURE) -> unsupported("OLE container (.xls or encrypted .xlsx)")

            // zip이 아닌 .apkg는 내용으로 다시 판정하지 않는다. 텍스트로 읽으면 깨진 카드가 만들어진다.
            extension == "apkg" || extension == "xlsx" -> corrupt("not a zip archive")

            else -> DetectedSource.Text
        }
    }

    private fun detectZip(archive: ZipArchive): DetectedSource =
        when {
            APKG_ENTRIES.any(archive::contains) -> DetectedSource.Apkg(archive)
            archive.contains(XLSX_WORKBOOK) -> DetectedSource.Xlsx(archive)
            else -> unsupported("zip without Anki collection or workbook")
        }

    private fun unsupported(reason: String): Nothing = throw ImportParseException(ImportParseErrorKind.UNSUPPORTED_FILE, reason)

    private fun corrupt(reason: String): Nothing = throw ImportParseException(ImportParseErrorKind.CORRUPT_FILE, reason)
}
