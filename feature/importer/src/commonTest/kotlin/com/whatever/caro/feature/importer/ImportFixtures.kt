package com.whatever.caro.feature.importer

import com.whatever.caro.core.importparser.ImportParser
import com.whatever.caro.core.importparser.ParseOptions
import com.whatever.caro.core.importparser.SpreadsheetDateStyle
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.feature.importer.platform.platformLegacyKoreanDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.gzip
import okio.use

internal object ImportFixtures {
    const val EMPTY_SIDE_KO = "빈 항목입니다"

    val fileSystem: FileSystem = FileSystem.SYSTEM
    private val root: Path = FIXTURE_ROOT.toPath()

    fun exists(relative: String): Boolean = fileSystem.exists(root / relative)

    fun expectedFiles(directory: String): List<Path> =
        fileSystem
            .list(root / "expected" / directory)
            .filter { path -> path.name.endsWith(".json") }
            .sortedBy { path -> path.name }

    fun readExpected(path: Path): JsonObject = Json.parseToJsonElement(fileSystem.read(path) { readUtf8() }).jsonObject

    /** 파서 모듈 JVM 테스트가 최신으로 유지하는 Anki 파싱 결과. 네이티브 라이브러리 없이 연결 후 처리를 검증하려고 쓴다. */
    fun snapshot(
        fixture: String,
        variant: String,
    ): ParsedTable {
        val path = root / "snapshots/apkg/${fixture.substringAfterLast('/')}.$variant.json.gz"
        return fileSystem
            .source(path)
            .gzip()
            .buffer()
            .use { source -> Json.decodeFromString(ParsedTable.serializer(), source.readUtf8()) }
    }

    suspend fun parse(
        relative: String,
        subdeckIds: Set<String>? = null,
        sheetIndex: Int = 0,
        dateStyle: SpreadsheetDateStyle = SpreadsheetDateStyle.YEAR_MONTH_DAY,
    ): ParsedTable =
        ImportParser(fileSystem).parseFile(
            path = root / relative,
            sourceName = relative.substringAfterLast('/'),
            options =
                ParseOptions(
                    maxRows = ImportLimits.MAX_CARDS + 1,
                    maxFileBytes = ImportLimits.MAX_FILE_BYTES,
                    tempDirectory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "caro-importer-test",
                    subdeckIds = subdeckIds,
                    sheetIndex = sheetIndex,
                    legacyKoreanDecoder = platformLegacyKoreanDecoder(),
                    dateStyle = dateStyle,
                ),
        )
}

internal fun JsonObject.string(key: String): String? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull

internal fun JsonObject.int(key: String): Int? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.intOrNull

internal fun JsonObject.boolean(key: String): Boolean? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.booleanOrNull

internal fun JsonObject.objects(key: String): List<JsonObject> = (this[key] as? JsonArray)?.map { it.jsonObject }.orEmpty()

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.strings(key: String): List<String?> =
    (this[key] as? JsonArray)?.map { element -> element.takeUnless { it is JsonNull }?.jsonPrimitive?.contentOrNull }.orEmpty()
