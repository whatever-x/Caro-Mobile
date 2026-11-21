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
import okio.FileSystem
import okio.Path
import okio.use

/** 미디어 항목, `media` 목록, 복습 기록과 스케줄 열은 가져오지 않으므로 열지도 않는다. 깨진 미디어가 있어도 카드를 가져올 수 있다. */
internal class ApkgReader(
    private val fileSystem: FileSystem,
    private val archive: ZipArchive,
    private val options: ParseOptions,
) {
    private val maxFieldBytes = options.maxFieldChars.toLong() * MAX_UTF8_BYTES_PER_CHAR

    private class RowScan(
        val rows: List<ParsedRow>,
        val fieldCountMismatches: Int,
        val limitReached: Boolean,
    )

    private class NoteDeck(
        val deck: Long,
        val cardCount: Int,
    )

    private class NoteSummary(
        val notesByDeck: Map<Long, Int>,
        val imageOcclusionNotesByDeck: Map<Long, Int>,
        val noteCounts: Map<Long, Int>,
        val selectedNoteCounts: Map<Long, Int>,
        val selectedCardCounts: Map<Long, Int>,
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
                AnkiCollectionModel.readModern(database, options.maxColumns, maxFieldBytes)
            } else {
                AnkiCollectionModel.readLegacy(database, options.maxColumns)
            }
        val noteDecks = readNoteDecks(database, model)
        val selectedDecks = selectedDecksOf(model)
        val summary = summarizeNotes(database, model, noteDecks, selectedDecks)
        val noteTypes = noteTypesOf(summary, model)
        val scan = readRows(database, model, noteDecks, selectedDecks)
        return ParsedTable(
            format = ImportFormat.APKG,
            sourceName = sourceName,
            htmlMode = HtmlMode.HTML,
            subdecks = subdecksOf(summary, model),
            noteTypes = noteTypes,
            rows = scan.rows,
            warnings =
                listOfNotNull(
                    countWarning(ParseWarningCode.APKG_FIELD_COUNT_MISMATCH, scan.fieldCountMismatches),
                    countWarning(
                        ParseWarningCode.APKG_IMAGE_OCCLUSION_EXCLUDED,
                        noteTypes.filter { type -> type.isImageOcclusion }.sumOf { type -> type.selectedCardCount ?: 0 },
                    ),
                ),
            rowLimitReached = scan.limitReached,
        )
    }

    /**
     * 노트의 하위 덱은 덱 목록에 있는 덱의 카드 가운데 ord가 가장 작은 카드의 덱이다. 필터 덱에 있는 카드는 원래 덱(odid)으로 센다.
     * 카드 수는 같은 nid의 행이 이어진다는 정렬 순서에 기대어 세므로 ORDER BY의 첫 열을 바꾸면 안 된다.
     */
    private fun readNoteDecks(
        database: AnkiDatabase,
        model: AnkiCollectionModel,
    ): Map<Long, NoteDeck> {
        val noteDecks = HashMap<Long, NoteDeck>()
        var noteId = 0L
        var deck: Long? = null
        var cardCount = 0

        fun store() {
            deck?.let { noteDeck -> noteDecks[noteId] = NoteDeck(noteDeck, cardCount) }
        }
        database.query("SELECT nid, did, odid FROM cards ORDER BY nid, ord") { statement ->
            while (statement.step()) {
                archive.ensureActive()
                val cardNoteId = statement.getLong(0)
                if (cardCount > 0 && cardNoteId != noteId) {
                    store()
                    deck = null
                    cardCount = 0
                }
                noteId = cardNoteId
                cardCount++
                val originalDeck = statement.getLong(2)
                val cardDeck = if (originalDeck != 0L) originalDeck else statement.getLong(1)
                if (deck == null && cardDeck in model.deckPaths) deck = cardDeck
            }
        }
        store()
        return noteDecks
    }

    private fun selectedDecksOf(model: AnkiCollectionModel): Set<Long> {
        val selection = options.subdeckIds ?: return model.deckPaths.keys
        return model.deckPaths.keys.filterTo(HashSet()) { deck -> deck.toString() in selection }
    }

    private fun subdecksOf(
        summary: NoteSummary,
        model: AnkiCollectionModel,
    ): List<ParsedSubdeck> =
        summary.notesByDeck
            .map { (deck, count) ->
                ParsedSubdeck(deck.toString(), model.deckPaths.getValue(deck), count, summary.imageOcclusionNotesByDeck[deck] ?: 0)
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { subdeck: ParsedSubdeck -> subdeck.path }.thenBy { it.path })

    /**
     * 선택 범위 노트 수와 카드 수는 maxRows로 잘리면 안 되므로 행 읽기와 따로 id와 유형만 읽어 센다.
     * 하위 덱 노트 수가 가져올 행 수와 어긋나지 않도록 [readRows]와 같은 조건(덱이 있고 유형을 아는 노트)으로 센다.
     */
    private fun summarizeNotes(
        database: AnkiDatabase,
        model: AnkiCollectionModel,
        noteDecks: Map<Long, NoteDeck>,
        selectedDecks: Set<Long>,
    ): NoteSummary {
        val notesByDeck = HashMap<Long, Int>()
        val imageOcclusionNotesByDeck = HashMap<Long, Int>()
        val noteCounts = HashMap<Long, Int>()
        val selectedNoteCounts = HashMap<Long, Int>()
        val selectedCardCounts = HashMap<Long, Int>()
        database.query("SELECT id, mid FROM notes") { statement ->
            while (statement.step()) {
                archive.ensureActive()
                val noteDeck = noteDecks[statement.getLong(0)] ?: continue
                val typeId = statement.getLong(1)
                val type = model.noteTypes[typeId] ?: continue
                notesByDeck.addTo(noteDeck.deck, 1)
                if (type.isImageOcclusion) imageOcclusionNotesByDeck.addTo(noteDeck.deck, 1)
                noteCounts.addTo(typeId, 1)
                if (noteDeck.deck !in selectedDecks) continue
                selectedNoteCounts.addTo(typeId, 1)
                selectedCardCounts.addTo(typeId, noteDeck.cardCount)
            }
        }
        return NoteSummary(notesByDeck, imageOcclusionNotesByDeck, noteCounts, selectedNoteCounts, selectedCardCounts)
    }

    private fun noteTypesOf(
        summary: NoteSummary,
        model: AnkiCollectionModel,
    ): List<ParsedNoteType> =
        summary.noteCounts.keys.sorted().map { id ->
            val type = model.noteTypes.getValue(id)
            ParsedNoteType(
                id = id.toString(),
                name = type.name,
                fieldNames = type.fieldNames,
                isCloze = type.isCloze,
                noteCount = summary.noteCounts.getValue(id),
                isImageOcclusion = type.isImageOcclusion,
                selectedNoteCount = summary.selectedNoteCounts[id] ?: 0,
                selectedCardCount = summary.selectedCardCounts[id] ?: 0,
            )
        }

    private fun MutableMap<Long, Int>.addTo(
        key: Long,
        amount: Int,
    ) {
        this[key] = (this[key] ?: 0) + amount
    }

    private suspend fun readRows(
        database: AnkiDatabase,
        model: AnkiCollectionModel,
        noteDecks: Map<Long, NoteDeck>,
        selectedDecks: Set<Long>,
    ): RowScan {
        val rows = mutableListOf<ParsedRow>()
        var mismatches = 0
        var cells = 0
        var chars = 0L
        database.query("SELECT id, mid, flds, octet_length(flds) FROM notes ORDER BY id") { statement ->
            while (rows.size < options.maxRows && statement.step()) {
                // 선택에서 빠지는 노트가 계속 이어져도 취소를 알아차리도록 행마다 확인한다.
                archive.ensureActive()
                val type = model.noteTypes[statement.getLong(1)] ?: continue
                val deck = noteDecks[statement.getLong(0)]?.deck ?: continue
                if (deck !in selectedDecks) continue
                // 그림 위 가림 좌표라 글자 카드로 옮길 수 없다.
                if (type.isImageOcclusion) continue
                // 값 문자열을 만들기 전에 바이트 길이로 거른다. 한 노트가 수백 MB면 읽는 순간 메모리가 넘친다.
                // length()는 내장 NUL에서 세기를 멈춰 값 앞에 NUL을 두면 0이 되므로 쓰지 않는다.
                if (statement.getLong(3) > maxFieldBytes) throw contentTooLong("note over ${options.maxFieldChars} chars")
                cells += type.fieldNames.size
                if (cells > options.maxCells) throw contentTooLong("table over ${options.maxCells} cells")
                val text = statement.getText(2)
                if (text.length > options.maxFieldChars) throw contentTooLong("note over ${options.maxFieldChars} chars")
                chars += text.length
                if (chars > options.maxTotalChars) throw contentTooLong("table over ${options.maxTotalChars} chars")
                val values = text.split(FIELD_SEPARATOR)
                if (values.size != type.fieldNames.size) mismatches++
                val fields = type.fieldNames.withIndex().associate { (index, name) -> name to values.getOrElse(index) { "" } }
                rows += ParsedRow(statement.getLong(1).toString(), deck.toString(), fields)
            }
        }
        // 선택한 노트가 상한 행을 넘으면 나머지는 읽지 않는다. 텍스트와 같이 상한 행에 닿으면 상한 초과로 본다.
        return RowScan(rows, mismatches, limitReached = rows.size >= options.maxRows)
    }

    private fun contentTooLong(reason: String) = ImportParseException(ImportParseErrorKind.CONTENT_TOO_LONG, reason)

    private companion object {
        const val COLLECTION_FILE = "collection.sqlite"
        const val FIELD_SEPARATOR = '\u001f'

        // UTF-8은 UTF-16 글자 하나를 최대 3바이트로 인코딩한다.
        const val MAX_UTF8_BYTES_PER_CHAR = 3
    }
}
