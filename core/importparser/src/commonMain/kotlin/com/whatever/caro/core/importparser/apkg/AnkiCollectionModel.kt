package com.whatever.caro.core.importparser.apkg

import com.whatever.caro.core.importparser.ImportParseErrorKind
import com.whatever.caro.core.importparser.ImportParseException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class AnkiNoteType(
    val name: String,
    val fieldNames: List<String>,
    val isCloze: Boolean,
    val isImageOcclusion: Boolean,
)

/** 노트 유형과 덱 경로. 최신 형식(스키마 18)은 테이블에, 구버전(스키마 11)은 `col` JSON에 있다. */
internal data class AnkiCollectionModel(
    val noteTypes: Map<Long, AnkiNoteType>,
    val deckPaths: Map<Long, String>,
) {
    companion object {
        private const val KIND_FIELD = 1
        private const val ORIGINAL_STOCK_KIND_FIELD = 9
        private const val KIND_CLOZE = 1L
        private const val STOCK_KIND_IMAGE_OCCLUSION = 6L
        private const val DECK_PATH_SEPARATOR = "::"
        private const val MODERN_DECK_SEPARATOR = '\u001f'
        private const val MAX_NOTE_TYPE_CONFIG_BYTES = 8L * 1024 * 1024

        /** 항목 수가 [maxColumns]를 넘는 노트 유형이 있으면 노트마다 그만큼의 값 맵이 생기므로 읽는 중에 멈춘다. */
        fun readModern(
            database: AnkiDatabase,
            maxColumns: Int,
        ): AnkiCollectionModel {
            val fieldNames = HashMap<Long, MutableList<String>>()
            // 내보내기가 없는 노트 유형의 fields 행을 남기므로 노트 유형 id로 묶는다.
            database.query("SELECT ntid, name FROM fields ORDER BY ntid, ord") { statement ->
                while (statement.step()) {
                    val names = fieldNames.getOrPut(statement.getLong(0)) { mutableListOf() }
                    if (names.size >= maxColumns) throw tooManyFields(maxColumns)
                    names += statement.getText(1)
                }
            }
            val noteTypes = HashMap<Long, AnkiNoteType>()
            database.query("SELECT id, name, config, octet_length(config) FROM notetypes") { statement ->
                while (statement.step()) {
                    val id = statement.getLong(0)
                    // getBlob은 값 전체를 힙에 올리므로 올리기 전에 크기로 거른다.
                    if (statement.getLong(3) > MAX_NOTE_TYPE_CONFIG_BYTES) {
                        throw ImportParseException(
                            ImportParseErrorKind.CONTENT_TOO_LONG,
                            "note type config over $MAX_NOTE_TYPE_CONFIG_BYTES bytes",
                        )
                    }
                    val config = ProtobufVarints.read(if (statement.isNull(2)) ByteArray(0) else statement.getBlob(2))
                    noteTypes[id] =
                        AnkiNoteType(
                            name = statement.getText(1),
                            fieldNames = fieldNames[id].orEmpty(),
                            isCloze = config[KIND_FIELD] == KIND_CLOZE,
                            isImageOcclusion = config[ORIGINAL_STOCK_KIND_FIELD] == STOCK_KIND_IMAGE_OCCLUSION,
                        )
                }
            }
            val deckPaths = HashMap<Long, String>()
            database.query("SELECT id, name FROM decks") { statement ->
                while (statement.step()) {
                    deckPaths[statement.getLong(0)] = statement.getText(1).replace(MODERN_DECK_SEPARATOR.toString(), DECK_PATH_SEPARATOR)
                }
            }
            return AnkiCollectionModel(noteTypes, deckPaths)
        }

        fun readLegacy(
            database: AnkiDatabase,
            maxColumns: Int,
        ): AnkiCollectionModel {
            val (models, decks) =
                database.query("SELECT models, decks FROM col") { statement ->
                    if (!statement.step()) error("col row missing")
                    statement.getText(0) to statement.getText(1)
                }
            val noteTypes =
                Json.parseToJsonElement(models).jsonObject.entries.associate { (id, model) ->
                    id.toLong() to legacyNoteType(model.jsonObject, maxColumns)
                }
            val deckPaths =
                Json.parseToJsonElement(decks).jsonObject.entries.associate { (id, deck) ->
                    id.toLong() to deck.jsonObject.text("name")
                }
            return AnkiCollectionModel(noteTypes, deckPaths)
        }

        private fun legacyNoteType(
            model: JsonObject,
            maxColumns: Int,
        ): AnkiNoteType {
            val declared = model["flds"]?.jsonArray
            if ((declared?.size ?: 0) > maxColumns) throw tooManyFields(maxColumns)
            val fields =
                declared
                    ?.map { it.jsonObject }
                    ?.sortedBy { field -> field.number("ord") }
                    ?.map { field -> field.text("name") }
                    .orEmpty()
            return AnkiNoteType(
                name = model.text("name"),
                fieldNames = fields,
                isCloze = model.number("type") == KIND_CLOZE.toInt(),
                isImageOcclusion = model.number("originalStockKind") == STOCK_KIND_IMAGE_OCCLUSION.toInt(),
            )
        }

        private fun tooManyFields(maxColumns: Int) =
            ImportParseException(ImportParseErrorKind.TOO_MANY_COLUMNS, "note type over $maxColumns fields")

        private fun JsonObject.text(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

        private fun JsonObject.number(key: String): Int? =
            (this[key] as? JsonPrimitive)?.let {
                it.intOrNull
                    ?: it.jsonPrimitive.contentOrNull?.toIntOrNull()
            }
    }
}
