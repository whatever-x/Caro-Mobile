package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParseWarning
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.transform.ImportCard
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap

private const val MIN_TYPES_FOR_NAME = 2

/** 선언 순서가 카드 아래에 놓이는 순서다. */
internal enum class CardFlag {
    TruncatedFront,
    TruncatedBack,
    MediaRemoved,
}

/** 빈 면은 값 자리에 빈 면 문구가 보이므로 표시를 따로 두지 않는다. */
internal fun cardFlags(card: ImportCard): List<CardFlag> =
    buildList {
        if (card.frontTruncated) add(CardFlag.TruncatedFront)
        if (card.backTruncated) add(CardFlag.TruncatedBack)
        if (card.mediaRemoved > 0) add(CardFlag.MediaRemoved)
    }

internal fun visibleFilters(counts: Map<ImportPreviewFilter, Int>): List<ImportPreviewFilter> =
    ImportPreviewFilter.entries.filter { filter -> (counts[filter] ?: 0) > 0 }

/** 선언 순서가 경고 줄이 쌓이는 순서다. */
internal enum class PreviewWarningKind {
    EncodingFallback,
    FormulaWithoutValue,
    NumberFormatRaw,
    FieldCountMismatch,
}

internal data class PreviewWarning(
    val kind: PreviewWarningKind,
    val count: Int,
)

/** Image Occlusion 제외 수는 설정 박스의 제외 줄이 보이므로 경고 줄에 겹쳐 쓰지 않는다. */
internal fun previewWarnings(warnings: List<ParseWarning>): List<PreviewWarning> =
    warnings
        .mapNotNull { warning -> warning.kind()?.let { kind -> PreviewWarning(kind, warning.count ?: 0) } }
        .sortedBy { warning -> warning.kind.ordinal }

private fun ParseWarning.kind(): PreviewWarningKind? =
    when (code) {
        ParseWarningCode.ENCODING_FALLBACK_CP949 -> PreviewWarningKind.EncodingFallback
        ParseWarningCode.XLSX_FORMULA_WITHOUT_VALUE -> PreviewWarningKind.FormulaWithoutValue
        ParseWarningCode.XLSX_NUMBER_FORMAT_RAW -> PreviewWarningKind.NumberFormatRaw
        ParseWarningCode.APKG_FIELD_COUNT_MISMATCH -> PreviewWarningKind.FieldCountMismatch
        else -> null
    }

/** 설정 박스의 유형 수와 카드의 유형 이름이 같은 기준을 써야 한다. Image Occlusion은 카드가 되지 않으므로 세지 않는다. */
internal fun importableNoteTypes(table: ParsedTable): List<ParsedNoteType> =
    mappingListNoteTypes(table).filterNot { type -> type.isImageOcclusion }

/** 유형이 하나뿐이면 모든 카드가 같은 유형이라 이름을 붙이지 않는다. */
internal fun cardTypeNames(table: ParsedTable): ImmutableMap<String, String> {
    val types = importableNoteTypes(table)
    if (types.size < MIN_TYPES_FOR_NAME) return persistentMapOf()
    return types.associate { type -> type.id to truncateNoteTypeName(type.name) }.toImmutableMap()
}

internal fun cardTypeName(
    table: ParsedTable,
    typeNames: Map<String, String>,
    card: ImportCard,
): String? =
    table.rows
        .getOrNull(card.sourceIndex)
        ?.noteTypeId
        ?.let(typeNames::get)
