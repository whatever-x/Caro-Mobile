package com.whatever.caro.feature.importer.transform

import com.whatever.caro.core.importparser.model.HtmlMode
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedRow
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.card.CardInputLimits
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.core.model.importing.ImportLimits
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** 서버는 글자 수와 빈 값을 검사하지 않으므로 500자 자르기, 빈 면 문구, 양쪽이 빈 카드 제외는 여기서만 보장된다. */
internal class ImportPreviewBuilder {
    private class Side(
        val text: String,
        val mediaRemoved: Int,
    )

    /** [emptySideText]는 가져올 때의 앱 언어 문구다. 저장 뒤에는 일반 텍스트가 된다. */
    suspend fun build(
        table: ParsedTable,
        mappings: Map<String, FieldMapping>,
        emptySideText: String,
        maxCards: Int = ImportLimits.MAX_CARDS,
    ): ImportPreview {
        val noteTypes = table.noteTypes.associateBy { type -> type.id }
        val cards = mutableListOf<ImportCard>()
        var excluded = 0
        table.rows.forEachIndexed { index, row ->
            if (index % CANCEL_CHECK_INTERVAL == 0) currentCoroutineContext().ensureActive()
            val type = noteTypes[row.noteTypeId] ?: return@forEachIndexed
            val mapping = mappings[row.noteTypeId] ?: defaultMapping(type)
            if (mapping.excluded) return@forEachIndexed
            val card = buildCard(index, row, table, type.isCloze, mapping, emptySideText)
            if (card == null) excluded++ else cards += card
        }
        val stats =
            ImportPreviewStats(
                cardCount = cards.size,
                excludedCount = excluded,
                emptyReplacedCount = cards.sumOf { card -> card.frontReplaced.toCount() + card.backReplaced.toCount() },
                truncatedCount = cards.sumOf { card -> card.frontTruncated.toCount() + card.backTruncated.toCount() },
                mediaRemovedCount = cards.sumOf { card -> card.mediaRemoved },
            )
        // 파서가 행 상한에서 읽기를 멈췄으면 읽지 않은 행이 있으므로 제외 카드로 장수가 줄어도 상한 초과로 본다.
        val limitExceeded = table.rowLimitReached || cards.size > maxCards
        return ImportPreview(cards.toImmutableList(), stats, table.warnings, limitExceeded)
    }

    private fun buildCard(
        index: Int,
        row: ParsedRow,
        table: ParsedTable,
        isClozeType: Boolean,
        mapping: FieldMapping,
        emptySideText: String,
    ): ImportCard? {
        val frontValues = mapping.frontFields.map { name -> row.fields[name].orEmpty() }
        val backValues = mapping.backFields.map { name -> row.fields[name].orEmpty() }
        // 텍스트 원본은 노트 유형이 하나라 앞면에 연결한 값의 표기로 행마다 빈칸 여부를 정한다.
        val cloze = isClozeType || (table.format == ImportFormat.TEXT && frontValues.any(ClozeTransformer::hasClozeMarkup))
        // 빈칸 문장은 앞면과 뒷면에서 두 번 정리되므로 미디어는 뒷면(채운 문장) 결과로 한 번만 센다.
        val front = side(if (cloze) frontValues.map(ClozeTransformer::front) else frontValues, table.htmlMode, countMedia = !cloze)
        val back = side(if (cloze) frontValues.map(ClozeTransformer::back) + backValues else backValues, table.htmlMode, countMedia = true)
        if (front.text.isEmpty() && back.text.isEmpty()) return null
        val (frontText, frontTruncated) = truncate(front.text.ifEmpty { emptySideText })
        val (backText, backTruncated) = truncate(back.text.ifEmpty { emptySideText })
        return ImportCard(
            sourceIndex = index,
            front = frontText,
            back = backText,
            frontReplaced = front.text.isEmpty(),
            backReplaced = back.text.isEmpty(),
            frontTruncated = frontTruncated,
            backTruncated = backTruncated,
            mediaRemoved = front.mediaRemoved + back.mediaRemoved,
        )
    }

    private fun side(
        values: List<String>,
        htmlMode: HtmlMode,
        countMedia: Boolean,
    ): Side {
        val cleaned = values.map { value -> HtmlCleaner.clean(value, htmlMode) }
        return Side(
            text = cleaned.map { it.text }.filter { text -> text.isNotBlank() }.joinToString("\n"),
            mediaRemoved = if (countMedia) cleaned.sumOf { it.mediaRemoved } else 0,
        )
    }

    /** 수동 카드 입력과 같은 UTF-16 코드 단위 기준이다. 서로게이트 쌍을 가르면 글자가 깨지므로 한 칸 앞에서 자른다. */
    private fun truncate(text: String): Pair<String, Boolean> {
        if (text.length <= CardInputLimits.FIELD_MAX) return text to false
        val cut = if (text[CardInputLimits.FIELD_MAX - 1].isHighSurrogate()) CardInputLimits.FIELD_MAX - 1 else CardInputLimits.FIELD_MAX
        return text.substring(0, cut) to true
    }

    private fun Boolean.toCount(): Int = if (this) 1 else 0

    private companion object {
        const val CANCEL_CHECK_INTERVAL = 256
    }
}

/** 첫 항목을 앞면(빈칸 유형은 빈칸 문장), 나머지를 뒷면(빈칸 유형은 추가 설명)에 둔다. */
internal fun defaultFieldMappings(table: ParsedTable): Map<String, FieldMapping> =
    table.noteTypes.associate { type ->
        type.id to
            defaultMapping(type)
    }

private fun defaultMapping(type: ParsedNoteType): FieldMapping =
    FieldMapping(
        noteTypeId = type.id,
        targets = mapOf(FieldMapping.FRONT to type.fieldNames.take(1), FieldMapping.BACK to type.fieldNames.drop(1)),
    )
