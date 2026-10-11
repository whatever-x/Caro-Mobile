package com.whatever.caro.core.importparser

import com.whatever.caro.core.importparser.model.ParsedTable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal object Fixtures {
    const val MAX_CARDS = 10_000
    const val MAX_ROWS = MAX_CARDS + 1
    const val MAX_FILE_BYTES = 104_857_600L

    val fileSystem: FileSystem = FileSystem.SYSTEM
    private val root: Path = FIXTURE_ROOT.toPath()
    val tempRoot: Path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-importparser-test"

    fun path(relative: String): Path = root / relative

    fun exists(relative: String): Boolean = fileSystem.exists(path(relative))

    fun expectedFiles(directory: String): List<Path> =
        fileSystem
            .list(root / "expected" / directory)
            .filter { path -> path.name.endsWith(".json") }
            .sortedBy { path -> path.name }

    fun readExpected(path: Path): JsonObject = Json.parseToJsonElement(fileSystem.read(path) { readUtf8() }).jsonObject

    fun options(
        subdeckIds: Set<String>? = null,
        sheetIndex: Int = 0,
        delimiter: Char? = null,
        hasHeaderRow: Boolean? = null,
        maxFileBytes: Long = MAX_FILE_BYTES,
        archiveLimits: ArchiveLimits = ArchiveLimits(),
        verifyIntegrity: Boolean = false,
        maxRows: Int = MAX_ROWS,
        maxColumns: Int = ParseOptions.DEFAULT_MAX_COLUMNS,
        maxFieldChars: Int = ParseOptions.DEFAULT_MAX_FIELD_CHARS,
        maxCells: Int = ParseOptions.DEFAULT_MAX_CELLS,
        maxTotalChars: Long = ParseOptions.DEFAULT_MAX_TOTAL_CHARS,
    ): ParseOptions =
        ParseOptions(
            maxRows = maxRows,
            maxColumns = maxColumns,
            maxFieldChars = maxFieldChars,
            maxCells = maxCells,
            maxTotalChars = maxTotalChars,
            maxFileBytes = maxFileBytes,
            tempDirectory = tempRoot,
            archiveLimits = archiveLimits,
            verifyIntegrity = verifyIntegrity,
            subdeckIds = subdeckIds,
            sheetIndex = sheetIndex,
            delimiter = delimiter,
            hasHeaderRow = hasHeaderRow,
            legacyKoreanDecoder = testLegacyKoreanDecoder(),
        )

    suspend fun parse(
        relative: String,
        options: ParseOptions = options(),
    ): ParsedTable =
        ImportParser(fileSystem).parseFile(
            path = path(relative),
            sourceName = relative.substringAfterLast('/'),
            options = options,
        )
}

internal fun JsonObject.string(key: String): String? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull

internal fun JsonObject.int(key: String): Int? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull

internal fun JsonObject.boolean(key: String): Boolean? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.booleanOrNull

internal fun JsonObject.objects(key: String): List<JsonObject> = (this[key] as? JsonArray)?.map { it.jsonObject }.orEmpty()

internal fun JsonObject.obj(key: String): JsonObject? = (this[key] as? JsonObject)

internal fun JsonObject.strings(key: String): List<String?> =
    (this[key] as? JsonArray)?.map { element -> element.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull }.orEmpty()

internal fun JsonElement.stringList(): List<String> = jsonArray.map { it.jsonPrimitive.content }
