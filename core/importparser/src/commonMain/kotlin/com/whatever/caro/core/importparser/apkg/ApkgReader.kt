package com.whatever.caro.core.importparser.apkg

import com.squareup.zstd.okio.zstdDecompress
import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import com.whatever.caro.core.importparser.ParseOptions
import com.whatever.caro.core.importparser.archive.ZipArchive
import com.whatever.caro.core.importparser.archive.withTempWorkspace
import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.importparser.model.countWarning
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.FileSystem
import okio.Path
import okio.use

/** 미디어 항목, `media` 목록, 복습 기록과 스케줄 열은 가져오지 않으므로 열지도 않는다. 깨진 미디어가 있어도 카드를 가져올 수 있다. */
internal class ApkgReader(
    private val fileSystem: FileSystem,
    private val archive: ZipArchive,
    private val options: ParseOptions,
) {
    private class RowScan(
        val rows: List<ParsedRow>,
        val fieldCountMismatches: Int,
        val imageOcclusionNotes: Int,
        val limitReached: Boolean,
    )

    suspend fun read(sourceName: String): ParsedTable {
        val version = AnkiPackageVersion.detect(archive)
        return withTempWorkspace(fileSystem, options.tempDirectory) { workspace ->
            val databasePath = workspace / COLLECTION_FILE
            extractCollection(version, databasePath)
            AnkiDatabase
                .open(options.sqliteDriver, databasePath, version.zstdCompressed, options.verifyIntegrity)
                .use { database -> readTable(database, version, sourceName) }
        }
    }

    private fun extractCollection(
        version: AnkiPackageVersion,
        target: Path,
    ) {
        archive.open(version.collectionEntry).use { entry ->
            val source = if (version.zstdCompressed) archive.limitSource(entry.zstdDecompress()) else entry
            source.use { content -> fileSystem.write(target) { writeAll(content) } }
        }
    }

    private suspend fun readTable(
        database: AnkiDatabase,
        version: AnkiPackageVersion,
        sourceName: String,
    ): ParsedTable {
        val model =
            if (version ==
                AnkiPackageVersion.LATEST
            ) {
                AnkiCollectionModel.readModern(database, options.maxColumns)
            } else {
                AnkiCollectionModel.readLegacy(database, options.maxColumns)
            }
        val noteDecks = readNoteDecks(database, model)
        val subdecks = subdecksOf(noteDecks, model)
        val selection = options.subdeckIds ?: subdecks.map { subdeck -> subdeck.id }.toSet()
        val scan = readRows(database, model, noteDecks, selection)
        return ParsedTable(
            format = ImportFormat.APKG,
            sourceName = sourceName,
            htmlMode = HtmlMode.HTML,
            subdecks = subdecks,
            noteTypes = readNoteTypes(database, model),
            rows = scan.rows,
            warnings =
                listOfNotNull(
                    countWarning(ParseWarningCode.APKG_FIELD_COUNT_MISMATCH, scan.fieldCountMismatches),
                    countWarning(ParseWarningCode.APKG_IMAGE_OCCLUSION_EXCLUDED, scan.imageOcclusionNotes),
                ),
            rowLimitReached = scan.limitReached,
        )
    }

    /** 노트의 하위 덱은 ord가 가장 작은 카드의 덱이다. 필터 덱에 있는 카드는 원래 덱(odid)으로 센다. */
    private fun readNoteDecks(
        database: AnkiDatabase,
        model: AnkiCollectionModel,
    ): Map<Long, Long> {
        val noteDecks = HashMap<Long, Long>()
        database.query("SELECT nid, did, odid FROM cards ORDER BY nid, ord") { statement ->
            while (statement.step()) {
                val noteId = statement.getLong(0)
                if (noteId in noteDecks) continue
                val originalDeck = statement.getLong(2)
                val deck = if (originalDeck != 0L) originalDeck else statement.getLong(1)
                if (deck in model.deckPaths) noteDecks[noteId] = deck
            }
        }
        return noteDecks
    }

    private fun subdecksOf(
        noteDecks: Map<Long, Long>,
        model: AnkiCollectionModel,
    ): List<ParsedSubdeck> =
        noteDecks.values
            .groupingBy { deck -> deck }
            .eachCount()
            .map { (deck, count) -> ParsedSubdeck(deck.toString(), model.deckPaths.getValue(deck), count) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { subdeck: ParsedSubdeck -> subdeck.path }.thenBy { it.path })

    private fun readNoteTypes(
        database: AnkiDatabase,
        model: AnkiCollectionModel,
    ): List<ParsedNoteType> {
        val counts = LinkedHashMap<Long, Int>()
        database.query("SELECT mid, count(*) FROM notes GROUP BY mid ORDER BY mid") { statement ->
            while (statement.step()) counts[statement.getLong(0)] = statement.getLong(1).toInt()
        }
        return counts.mapNotNull { (id, count) ->
            model.noteTypes[id]?.let { type -> ParsedNoteType(id.toString(), type.name, type.fieldNames, type.isCloze, count) }
        }
    }

    private suspend fun readRows(
        database: AnkiDatabase,
        model: AnkiCollectionModel,
        noteDecks: Map<Long, Long>,
        selection: Set<String>,
    ): RowScan {
        val rows = mutableListOf<ParsedRow>()
        var mismatches = 0
        var imageOcclusion = 0
        var cells = 0
        database.query("SELECT id, mid, flds, length(flds) FROM notes ORDER BY id") { statement ->
            while (rows.size < options.maxRows && statement.step()) {
                val type = model.noteTypes[statement.getLong(1)] ?: continue
                val subdeckId = noteDecks[statement.getLong(0)]?.toString()
                if ((subdeckId ?: ParsedSubdeck.NO_DECK_ID) !in selection) continue
                // 그림 위 가림 좌표라 글자 카드로 옮길 수 없다.
                if (type.isImageOcclusion) {
                    imageOcclusion++
                    continue
                }
                // 값 문자열을 만들기 전에 길이로 거른다. 한 노트가 수백 MB면 읽는 순간 메모리가 넘친다.
                if (statement.getLong(3) > options.maxFieldChars) throw contentTooLong("note over ${options.maxFieldChars} chars")
                cells += type.fieldNames.size
                if (cells > options.maxCells) throw contentTooLong("table over ${options.maxCells} cells")
                val values = statement.getText(2).split(FIELD_SEPARATOR)
                if (values.size != type.fieldNames.size) mismatches++
                val fields = type.fieldNames.withIndex().associate { (index, name) -> name to values.getOrElse(index) { "" } }
                rows += ParsedRow(statement.getLong(1).toString(), subdeckId, fields)
                if (rows.size % CANCEL_CHECK_INTERVAL == 0) currentCoroutineContext().ensureActive()
            }
        }
        // 선택한 노트가 상한 행을 넘으면 나머지는 읽지 않는다. 텍스트와 같이 상한 행에 닿으면 상한 초과로 본다.
        return RowScan(rows, mismatches, imageOcclusion, limitReached = rows.size >= options.maxRows)
    }

    private fun contentTooLong(reason: String) = ImportParseException(ImportParseErrorKind.CONTENT_TOO_LONG, reason)

    private companion object {
        const val COLLECTION_FILE = "collection.sqlite"
        const val FIELD_SEPARATOR = '\u001f'
        const val CANCEL_CHECK_INTERVAL = 256
    }
}
