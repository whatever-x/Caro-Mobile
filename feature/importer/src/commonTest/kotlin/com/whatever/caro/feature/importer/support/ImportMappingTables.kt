package com.whatever.caro.feature.importer.support

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable

/** 장수를 손으로 셀 수 있는 작은 Anki 표. 스냅샷 fixture는 장수 기대값을 구현에서 다시 계산하게 되므로 쓰지 않는다. */
internal object ImportMappingTables {
    const val BASIC = "basic"
    const val VOCAB = "vocab"
    private const val ROWS_PER_SUBDECK = 2

    /** [paths]의 하위 덱마다 Basic 노트 둘이 있다. 하위 덱을 다시 읽은 결과 모양이라 행이 채워져 있다. */
    fun deckTable(
        sourceName: String,
        vararg paths: String,
    ): ParsedTable {
        val subdecks = paths.mapIndexed { index, path -> ParsedSubdeck(id = "d${index + 1}", path = path, noteCount = ROWS_PER_SUBDECK) }
        return ParsedTable(
            format = ImportFormat.APKG,
            sourceName = sourceName,
            htmlMode = HtmlMode.PLAIN,
            subdecks = subdecks,
            noteTypes = listOf(basicType(noteCount = subdecks.size * ROWS_PER_SUBDECK)),
            rows =
                subdecks.flatMap { subdeck ->
                    List(ROWS_PER_SUBDECK) { n ->
                        ParsedRow(BASIC, subdeck.id, mapOf("Front" to "${subdeck.id}-앞$n", "Back" to "${subdeck.id}-뒤$n"))
                    }
                },
        )
    }

    /** 한 하위 덱에 Basic 노트 셋과 Vocab 노트 둘이 있다. 기본 연결의 카드는 5장, Vocab을 끄면 3장이다. */
    fun twoTypeTable(sourceName: String = "mixed.apkg"): ParsedTable =
        ParsedTable(
            format = ImportFormat.APKG,
            sourceName = sourceName,
            htmlMode = HtmlMode.PLAIN,
            subdecks = listOf(ParsedSubdeck(id = "d1", path = "Mixed", noteCount = 5)),
            noteTypes = listOf(basicType(noteCount = 3), vocabType(noteCount = 2)),
            rows =
                List(3) { n -> ParsedRow(BASIC, "d1", mapOf("Front" to "f$n", "Back" to "b$n")) } +
                    List(2) { n -> ParsedRow(VOCAB, "d1", mapOf("Word" to "w$n", "Meaning" to "m$n")) },
        )

    /** 하위 덱 선택 단계가 돌려주는 모양이다. 행과 경고를 비운다. */
    fun summaryOf(table: ParsedTable): ParsedTable = table.copy(rows = emptyList(), rowLimitReached = false, warnings = emptyList())

    /** 모든 칸이 비어 있어 기본 연결로 만들면 카드가 0장이 되는 텍스트 표. */
    fun blankRowsText(rows: Int = 3): ParsedTable =
        ImportTestTables.text(rows = rows).let { table ->
            table.copy(rows = table.rows.map { row -> row.copy(fields = row.fields.mapValues { "" }) })
        }

    /** 열이 셋인 텍스트 표. 같은 노트 유형 id로 열 이름만 달라진 다시 읽기 결과를 만든다. */
    fun threeColumnText(rows: Int = 3): ParsedTable =
        ImportTestTables.text(rows = rows, delimiter = ",").let { table ->
            table.copy(
                noteTypes = table.noteTypes.map { type -> type.copy(fieldNames = type.fieldNames + "열 3") },
                rows = table.rows.map { row -> row.copy(fields = row.fields + ("열 3" to "셋째")) },
            )
        }

    private fun basicType(noteCount: Int) = ParsedNoteType(BASIC, "Basic", listOf("Front", "Back"), isCloze = false, noteCount = noteCount)

    private fun vocabType(noteCount: Int) =
        ParsedNoteType(VOCAB, "Vocab", listOf("Word", "Meaning"), isCloze = false, noteCount = noteCount)
}
