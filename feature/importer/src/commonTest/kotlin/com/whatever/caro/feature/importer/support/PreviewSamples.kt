package com.whatever.caro.feature.importer.support

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParseWarning
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.core.importparser.model.ParsedLayout
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.logic.deckNamePrefill
import com.whatever.caro.feature.importer.logic.filterCards
import com.whatever.caro.feature.importer.logic.previewFilterCounts
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.ImportStep
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.ImportPreviewStats
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet

/**
 * 미리보기 화면 테스트와 스크린샷이 함께 쓰는 표와 카드다. 값은 Figma 미리보기 화면의 예시를 따른다.
 * 카드의 sourceIndex는 표의 행 위치와 같아야 카드 유형을 찾을 수 있다.
 */
internal object PreviewSamples {
    const val EMPTY_SIDE = "(비어 있음)"
    const val ANKI_FILE = "JLPT N5 단어.apkg"
    const val XLSX_FILE = "토익 단어장.xlsx"
    const val TEXT_FILE = "영단어.csv"
    const val PASTE_DECK_NAME = "붙여 넣은 카드"
    const val ANKI_TYPE = "일본어 단어"
    const val BASIC_TYPE = "기본"
    const val IMAGE_OCCLUSION_CARDS = 12

    private const val ANKI_TYPE_ID = "vocab"
    private const val BASIC_TYPE_ID = "basic"
    private const val IMAGE_OCCLUSION_TYPE_ID = "io"
    private const val SHEET_TYPE_ID = "sheet"
    private const val TEXT_TYPE_ID = "text"

    private val JAPAN_BACK = "일본\n" + "日本は東アジアにある島国で、北海道、本州、四国、九州の四つの大きな島からなります。".repeat(3)
    private val TOKYO_BACK = "도쿄\n" + "東京は日本の首都で、政治、経済、文化の中心地です。".repeat(4)

    val ankiCards =
        listOf(
            card(0, "学校", "학교\n学校に行きます。\nがっこう"),
            card(1, "読む", EMPTY_SIDE, backReplaced = true),
            card(2, "日本", JAPAN_BACK, backTruncated = true),
            card(3, "犬", "개\n犬が好きです。\nいぬ", mediaRemoved = 1),
            card(4, "猫", "고양이\n猫が寝ています。\nねこ", mediaRemoved = 2),
            card(5, "東京", TOKYO_BACK, frontTruncated = true, backTruncated = true),
        )

    val ankiTable =
        ParsedTable(
            format = ImportFormat.APKG,
            sourceName = ANKI_FILE,
            htmlMode = HtmlMode.HTML,
            subdecks =
                listOf(
                    ParsedSubdeck(id = "nouns", path = "JLPT N5::명사", noteCount = 7),
                    ParsedSubdeck(id = "verbs", path = "JLPT N5::동사", noteCount = 5, imageOcclusionNoteCount = 4),
                ),
            noteTypes =
                listOf(
                    noteType(ANKI_TYPE_ID, ANKI_TYPE, listOf("단어", "뜻", "예문", "읽기"), notes = 4),
                    noteType(BASIC_TYPE_ID, BASIC_TYPE, listOf("앞면", "뒷면"), notes = 4),
                    noteType(IMAGE_OCCLUSION_TYPE_ID, "Image Occlusion", listOf("Image", "Header"), notes = 4, isImageOcclusion = true),
                ),
            rows = rows(listOf(ANKI_TYPE_ID, ANKI_TYPE_ID, BASIC_TYPE_ID, ANKI_TYPE_ID, ANKI_TYPE_ID, BASIC_TYPE_ID), fieldCount = 2),
            warnings = listOf(ParseWarning(ParseWarningCode.APKG_IMAGE_OCCLUSION_EXCLUDED, count = IMAGE_OCCLUSION_CARDS)),
        )

    val xlsxCards =
        listOf(
            card(0, "abandon", "버리다\nHe abandoned the plan."),
            card(1, "benefit", "이익, 혜택\nThe new policy has many benefits."),
            card(2, "deadline", EMPTY_SIDE, backReplaced = true),
            card(3, "estimate", "추정하다\nWe estimated the cost."),
        )

    val xlsxTable =
        ParsedTable(
            format = ImportFormat.XLSX,
            sourceName = XLSX_FILE,
            htmlMode = HtmlMode.PLAIN,
            sheets =
                listOf(
                    ParsedSheet(index = 0, name = "Day1", rowCount = 5),
                    ParsedSheet(index = 1, name = "Day2", rowCount = 802),
                    ParsedSheet(index = 2, name = "메모", rowCount = 0),
                ),
            noteTypes = listOf(noteType(SHEET_TYPE_ID, "Day1", listOf("단어", "뜻", "예문"), notes = 4)),
            rows = rows(List(xlsxCards.size) { SHEET_TYPE_ID }, fieldCount = 3),
            layout = ParsedLayout(delimiter = null, hasHeaderRow = true),
        )

    val xlsxWarnings =
        listOf(
            ParseWarning(ParseWarningCode.XLSX_NUMBER_FORMAT_RAW),
            ParseWarning(ParseWarningCode.XLSX_FORMULA_WITHOUT_VALUE, count = 3),
        )

    val textCards =
        listOf(
            card(0, "apple", "사과"),
            card(1, "book", "책"),
            card(2, "cat", "고양이"),
            card(3, "dog", "개"),
        )

    val textTable = textTable(TEXT_FILE, delimiter = ",", fieldCount = 2, singleFieldRows = 0)

    val pasteCards =
        listOf(
            card(0, "사과", "apple"),
            card(1, "바나나", "banana"),
            card(2, "포도", "grape"),
        )

    val pasteTable = textTable(PASTE_DECK_NAME, delimiter = "\t", fieldCount = 2, singleFieldRows = 0, rowCount = pasteCards.size)

    /** 모든 줄이 한 칸이라 구분 기준을 확인하라는 띠가 켜진다. */
    val singleColumnCards =
        listOf(
            card(0, "apple - 사과", EMPTY_SIDE, backReplaced = true),
            card(1, "book - 책", EMPTY_SIDE, backReplaced = true),
            card(2, "cat - 고양이", EMPTY_SIDE, backReplaced = true),
        )

    val singleColumnTable =
        textTable(TEXT_FILE, delimiter = ",", fieldCount = 1, singleFieldRows = singleColumnCards.size, rowCount = singleColumnCards.size)

    fun preview(
        cards: List<ImportCard>,
        excluded: Int = 0,
        warnings: List<ParseWarning> = emptyList(),
    ): ImportPreview =
        ImportPreview(
            cards = cards.toImmutableList(),
            stats =
                ImportPreviewStats(
                    cardCount = cards.size,
                    excludedCount = excluded,
                    emptyReplacedCount = cards.count { card -> card.frontReplaced || card.backReplaced },
                    truncatedCount = cards.count { card -> card.frontTruncated || card.backTruncated },
                    mediaRemovedCount = cards.sumOf { card -> card.mediaRemoved },
                ),
            warnings = warnings,
            limitExceeded = false,
        )

    val ankiPreview: ImportPreview get() = preview(ankiCards, excluded = 2, warnings = ankiTable.warnings)
    val xlsxPreview: ImportPreview get() = preview(xlsxCards, excluded = 3)
    val textPreview: ImportPreview get() = preview(textCards)
    val pastePreview: ImportPreview get() = preview(pasteCards)
    val singleColumnPreview: ImportPreview get() = preview(singleColumnCards)

    /** ViewModel이 미리보기를 연 직후와 같은 모양이다. 칩 수와 거른 목록도 ViewModel과 같은 함수로 채운다. */
    fun state(
        table: ParsedTable,
        preview: ImportPreview,
        isPaste: Boolean = false,
        filter: ImportPreviewFilter = ImportPreviewFilter.All,
        detailCard: ImportCard? = null,
    ): ImportDeckState {
        val selection = ImportSelection(subdeckIds = table.subdecks.map { subdeck -> subdeck.id }.toImmutableSet())
        return ImportDeckState(
            step = ImportStep.Preview,
            isPaste = isPaste,
            table = table,
            selection = selection,
            mappings = defaultFieldMappings(table).toImmutableMap(),
            preview = preview,
            previewFilter = filter,
            filterCounts = previewFilterCounts(preview.cards),
            filteredCards = filterCards(preview.cards, filter),
            detailCard = detailCard,
            deckName = deckNamePrefill(table, isPaste, selection),
        )
    }

    fun card(
        index: Int,
        front: String,
        back: String,
        frontReplaced: Boolean = false,
        backReplaced: Boolean = false,
        frontTruncated: Boolean = false,
        backTruncated: Boolean = false,
        mediaRemoved: Int = 0,
    ) = ImportCard(
        sourceIndex = index,
        front = front,
        back = back,
        frontReplaced = frontReplaced,
        backReplaced = backReplaced,
        frontTruncated = frontTruncated,
        backTruncated = backTruncated,
        mediaRemoved = mediaRemoved,
    )

    private fun textTable(
        sourceName: String,
        delimiter: String,
        fieldCount: Int,
        singleFieldRows: Int,
        rowCount: Int = textCards.size,
    ): ParsedTable {
        val fieldNames = List(fieldCount) { index -> "열 ${index + 1}" }
        return ParsedTable(
            format = ImportFormat.TEXT,
            sourceName = sourceName,
            htmlMode = HtmlMode.PLAIN,
            noteTypes = listOf(noteType(TEXT_TYPE_ID, sourceName, fieldNames, notes = rowCount)),
            rows = rows(List(rowCount) { TEXT_TYPE_ID }, fieldCount),
            layout = ParsedLayout(delimiter = delimiter, hasHeaderRow = false, singleFieldRowCount = singleFieldRows),
        )
    }

    private fun noteType(
        id: String,
        name: String,
        fieldNames: List<String>,
        notes: Int,
        isImageOcclusion: Boolean = false,
    ) = ParsedNoteType(
        id = id,
        name = name,
        fieldNames = fieldNames,
        isCloze = false,
        noteCount = notes,
        isImageOcclusion = isImageOcclusion,
        selectedNoteCount = notes,
        selectedCardCount = if (isImageOcclusion) IMAGE_OCCLUSION_CARDS else notes,
    )

    /** 미리보기 카드는 따로 고정하므로 행 값은 유형을 가리키는 데만 쓴다. */
    private fun rows(
        noteTypeIds: List<String>,
        fieldCount: Int,
    ): List<ParsedRow> =
        noteTypeIds.mapIndexed { index, id ->
            ParsedRow(noteTypeId = id, fields = List(fieldCount) { field -> "열 ${field + 1}" to "값 $index" }.toMap())
        }
}
