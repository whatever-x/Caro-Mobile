package com.whatever.caro.feature.importer.support

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.feature.importer.logic.draftSettingsReason
import com.whatever.caro.feature.importer.logic.firstPreviewCards
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.mvi.SettingsEntry
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.mvi.SettingsReason
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet

/**
 * 설정 화면 테스트와 스크린샷이 함께 쓰는 표다. 값은 Figma 설정 변경과 먼저 고칠 것 화면의 예시를 따른다.
 * 사유와 첫 카드는 ViewModel과 같은 함수로 채워, 화면만 보고도 ViewModel이 만들 상태와 같다.
 */
internal object SettingsSamples {
    const val ANKI_FILE = "JLPT N5 단어.apkg"
    const val XLSX_FILE = "토익 단어장.xlsx"
    const val GATE_XLSX_FILE = "JLPT N5 단어장.xlsx"
    const val LONG_SUBDECK = "Day 01 - 기초 동사와 형용사 활용(て형, た형, ない형)과 자주 틀리는 조사 연습 문제 모음, 복습용 예문 포함"
    const val VOCAB_TYPE_ID = "vocab"
    const val CLOZE_TYPE_ID = "cloze"
    const val BASIC_TYPE_ID = "basic"
    const val MANY_TYPE_ID = "many"
    const val TEXT_ROW_COUNT = 45
    const val IMAGE_OCCLUSION_CARDS = 12

    private const val EMPTY_SIDE = "(비어 있음)"
    private const val IMAGE_OCCLUSION_TYPE_ID = "io"
    private const val SHEET_TYPE_ID = "sheet"
    private const val TEXT_TYPE_ID = "text"
    private val FRUITS = listOf("사과" to "apple", "바나나" to "banana", "포도" to "grape", "딸기" to "strawberry")

    val manyFieldNames = listOf("단어", "뜻", "예문", "읽기", "품사", "한자", "음성", "메모") + List(12) { index -> "추가 ${index + 1}" }

    val subdeckTable: ParsedTable =
        ankiTable(subdecks = listOf(subdeck("words", "JLPT N5::단어", 1_204), subdeck("grammar", "JLPT N5::문법", 310)))

    val partialSubdeckTable: ParsedTable =
        ankiTable(
            subdecks =
                listOf(
                    subdeck("words", "JLPT N5::단어", 1_204),
                    subdeck("grammar", "JLPT N5::문법", 310),
                    subdeck("day01", "JLPT N5::$LONG_SUBDECK", 520),
                ),
        )

    val overLimitSubdeckTable: ParsedTable =
        ankiTable(subdecks = listOf(subdeck("words", "JLPT N5::단어", 9_800), subdeck("grammar", "JLPT N5::문법", 2_600)))

    val sheetTable: ParsedTable =
        sheetTable(XLSX_FILE, listOf("Day1" to 864, "Day2" to 802, "메모" to 0), fieldNames = listOf("단어", "뜻", "예문"))

    val overLimitSheetTable: ParsedTable =
        sheetTable(GATE_XLSX_FILE, listOf("전체 단어" to 12_000, "N5 단어" to 820, "N4 단어" to 640, "메모" to 0), fieldNames = listOf("단어", "뜻"))

    val textTable: ParsedTable =
        ParsedTable(
            format = ImportFormat.TEXT,
            sourceName = "영단어.csv",
            htmlMode = HtmlMode.PLAIN,
            noteTypes = listOf(noteType(TEXT_TYPE_ID, "영단어.csv", listOf("열 1", "열 2"), TEXT_ROW_COUNT)),
            rows =
                List(TEXT_ROW_COUNT) { index ->
                    val (front, back) = FRUITS.getOrElse(index) { "단어 ${index + 1}" to "word ${index + 1}" }
                    ParsedRow(noteTypeId = TEXT_TYPE_ID, fields = mapOf("열 1" to front, "열 2" to back))
                },
            layout = ParsedLayout(delimiter = "\t", hasHeaderRow = false),
        )

    val mappingTable: ParsedTable =
        mappingTable(
            vocabType(),
            noteType(CLOZE_TYPE_ID, "빈칸 채우기", listOf("텍스트", "뒷면 추가"), 110, isCloze = true),
            noteType(BASIC_TYPE_ID, "기본", listOf("앞면", "뒷면"), 200),
            imageOcclusionType(),
        )

    val imageOcclusionMappingTable: ParsedTable = mappingTable(noteType(BASIC_TYPE_ID, "기본", listOf("앞면", "뒷면"), 200), imageOcclusionType())

    val singleTypeTable: ParsedTable = sheetTable(XLSX_FILE, listOf("Day1" to 5), fieldNames = listOf("단어", "뜻", "예문"))

    val manyFieldsTable: ParsedTable = mappingTable(noteType(MANY_TYPE_ID, "일본어 단어", manyFieldNames, 1_204))

    val manyFieldsMapping: Map<String, FieldMapping> =
        mapOf(
            MANY_TYPE_ID to
                FieldMapping(MANY_TYPE_ID, mapOf(FieldMapping.FRONT to listOf("단어"), FieldMapping.BACK to listOf("뜻", "예문", "읽기", "품사"))),
        )

    fun subdeckState(
        table: ParsedTable = subdeckTable,
        selected: Set<String> = table.subdecks.map { subdeck -> subdeck.id }.toSet(),
        entry: SettingsEntry = SettingsEntry.FromPreview,
    ): ImportDeckState = state(SettingsKind.Subdeck, entry, table, ImportSelection(subdeckIds = selected.toImmutableSet()))

    fun sheetState(
        table: ParsedTable = sheetTable,
        sheetIndex: Int = 0,
        entry: SettingsEntry = SettingsEntry.FromPreview,
    ): ImportDeckState = state(SettingsKind.Sheet, entry, table, ImportSelection(sheetIndex = sheetIndex))

    fun delimiterState(
        table: ParsedTable = textTable,
        pendingDelimiter: Char? = null,
    ): ImportDeckState {
        val draft = state(SettingsKind.Delimiter, SettingsEntry.FromPreview, table, ImportSelection())
        return draft.copy(pendingDraftSelection = pendingDelimiter?.let { delimiter -> draft.draftSelection.copy(delimiter = delimiter) })
    }

    fun mappingState(
        table: ParsedTable = mappingTable,
        mappings: Map<String, FieldMapping> = defaultFieldMappings(table),
    ): ImportDeckState = state(SettingsKind.CardLayout, SettingsEntry.FromPreview, table, ImportSelection(), mappings)

    fun excluded(table: ParsedTable): Map<String, FieldMapping> =
        defaultFieldMappings(table).mapValues { (_, mapping) -> mapping.copy(excluded = true) }

    private fun state(
        kind: SettingsKind,
        entry: SettingsEntry,
        table: ParsedTable,
        selection: ImportSelection,
        mappings: Map<String, FieldMapping> = defaultFieldMappings(table),
    ): ImportDeckState {
        val showsFirstCard = kind == SettingsKind.Delimiter || kind == SettingsKind.CardLayout
        return ImportDeckState(
            step = ImportStep.Settings(kind, entry),
            table = table.takeIf { entry == SettingsEntry.FromPreview },
            draftTable = table,
            draftSelection = selection,
            draftMappings = mappings.toImmutableMap(),
            draftFirstCards = if (showsFirstCard) firstPreviewCards(table, mappings, EMPTY_SIDE) else persistentMapOf(),
            settingsReason = draftSettingsReason(kind, table, selection, mappings, ImportLimits.MAX_CARDS),
        )
    }

    /** 사유를 바로 넣은 상태다. 카드를 만들어 봐야 아는 사유는 [적용] 뒤에만 생긴다. */
    fun withReason(
        state: ImportDeckState,
        reason: SettingsReason,
    ): ImportDeckState = state.copy(settingsReason = reason)

    private fun ankiTable(subdecks: List<ParsedSubdeck>): ParsedTable =
        ParsedTable(
            format = ImportFormat.APKG,
            sourceName = ANKI_FILE,
            htmlMode = HtmlMode.HTML,
            subdecks = subdecks,
            noteTypes = listOf(noteType(BASIC_TYPE_ID, "기본", listOf("앞면", "뒷면"), subdecks.sumOf { subdeck -> subdeck.noteCount })),
            rows = emptyList(),
        )

    private fun sheetTable(
        sourceName: String,
        sheets: List<Pair<String, Int>>,
        fieldNames: List<String>,
    ): ParsedTable =
        ParsedTable(
            format = ImportFormat.XLSX,
            sourceName = sourceName,
            htmlMode = HtmlMode.PLAIN,
            sheets = sheets.mapIndexed { index, (name, rows) -> ParsedSheet(index = index, name = name, rowCount = rows) },
            noteTypes = listOf(noteType(SHEET_TYPE_ID, sheets.first().first, fieldNames, 4)),
            rows =
                listOf(
                    ParsedRow(
                        noteTypeId = SHEET_TYPE_ID,
                        fields = fieldNames.zip(listOf("abandon", "버리다", "He abandoned the plan.")).toMap(),
                    ),
                ),
            layout = ParsedLayout(delimiter = null, hasHeaderRow = true),
        )

    /** 객체 초기화 순서상 아래 표들이 이 값을 쓰기 전에 만들어져 있어야 하므로 함수로 둔다. */
    private fun firstRow(noteTypeId: String): ParsedRow? =
        when (noteTypeId) {
            VOCAB_TYPE_ID -> row(VOCAB_TYPE_ID, "단어" to "学校", "뜻" to "학교", "예문" to "学校に行きます。", "읽기" to "がっこう")
            CLOZE_TYPE_ID -> row(CLOZE_TYPE_ID, "텍스트" to "毎朝{{c1::パン}}を食べます。", "뒷면 추가" to "아침마다 빵을 먹어요.")
            BASIC_TYPE_ID -> row(BASIC_TYPE_ID, "앞면" to "水", "뒷면" to "물")
            MANY_TYPE_ID -> row(MANY_TYPE_ID, "단어" to "学校", "뜻" to "학교", "예문" to "学校に行きます。", "읽기" to "がっこう", "품사" to "명사")
            else -> null
        }

    private fun mappingTable(vararg types: ParsedNoteType): ParsedTable =
        ParsedTable(
            format = ImportFormat.APKG,
            sourceName = ANKI_FILE,
            htmlMode = HtmlMode.HTML,
            noteTypes = types.toList(),
            rows = types.mapNotNull { type -> firstRow(type.id) },
        )

    private fun row(
        noteTypeId: String,
        vararg fields: Pair<String, String>,
    ): ParsedRow = ParsedRow(noteTypeId = noteTypeId, fields = fields.toMap())

    private fun vocabType(): ParsedNoteType = noteType(VOCAB_TYPE_ID, "일본어 단어", listOf("단어", "뜻", "예문", "읽기"), 1_204)

    private fun imageOcclusionType(): ParsedNoteType =
        noteType(IMAGE_OCCLUSION_TYPE_ID, "Image Occlusion", listOf("Image", "Header"), 4, isImageOcclusion = true)
            .copy(selectedCardCount = IMAGE_OCCLUSION_CARDS)

    private fun subdeck(
        id: String,
        path: String,
        notes: Int,
    ): ParsedSubdeck = ParsedSubdeck(id = id, path = path, noteCount = notes)

    private fun noteType(
        id: String,
        name: String,
        fieldNames: List<String>,
        notes: Int,
        isCloze: Boolean = false,
        isImageOcclusion: Boolean = false,
    ): ParsedNoteType =
        ParsedNoteType(
            id = id,
            name = name,
            fieldNames = fieldNames,
            isCloze = isCloze,
            noteCount = notes,
            isImageOcclusion = isImageOcclusion,
            selectedNoteCount = notes,
        )
}
