package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParseWarning
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.feature.importer.mvi.ImportPreviewFilter
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview

/** 선언 순서가 카드 아래에 보이는 순서다. */
internal enum class ChangeBadge {
    EmptyFront,
    EmptyBack,
    TruncatedFront,
    TruncatedBack,
    TruncatedBoth,
    MediaRemoved,
}

/** 지운 미디어는 카드당 값 하나라 면을 나누지 않고, 두 면이 모두 잘리면 배지 하나로 합친다. */
internal fun changeBadges(card: ImportCard): List<ChangeBadge> =
    buildList {
        if (card.frontReplaced) add(ChangeBadge.EmptyFront)
        if (card.backReplaced) add(ChangeBadge.EmptyBack)
        when {
            card.frontTruncated && card.backTruncated -> add(ChangeBadge.TruncatedBoth)
            card.frontTruncated -> add(ChangeBadge.TruncatedFront)
            card.backTruncated -> add(ChangeBadge.TruncatedBack)
        }
        if (card.mediaRemoved > 0) add(ChangeBadge.MediaRemoved)
    }

/** 해당 카드가 없는 칩은 숨긴다. 카드에서 직접 정해 미디어가 없는 텍스트 원본에는 미디어 제거 칩이 생기지 않는다. */
internal fun visibleFilters(preview: ImportPreview): List<ImportPreviewFilter> =
    ImportPreviewFilter.entries.filter { filter ->
        filter == ImportPreviewFilter.All || preview.cards.any { card -> card.matches(filter) }
    }

internal fun filterCards(
    cards: List<ImportCard>,
    filter: ImportPreviewFilter,
): List<ImportCard> = if (filter == ImportPreviewFilter.All) cards else cards.filter { card -> card.matches(filter) }

private fun ImportCard.matches(filter: ImportPreviewFilter): Boolean =
    when (filter) {
        ImportPreviewFilter.All -> true
        ImportPreviewFilter.EmptyReplaced -> frontReplaced || backReplaced
        ImportPreviewFilter.Truncated -> frontTruncated || backTruncated
        ImportPreviewFilter.MediaRemoved -> mediaRemoved > 0
    }

/** 선언 순서가 경고 줄이 쌓이는 순서다. */
internal enum class PreviewWarningKind {
    EncodingFallback,
    InconsistentColumns,
    FormulaWithoutValue,
    NumberFormatRaw,
    FieldCountMismatch,
    ImageOcclusionExcluded,
}

internal data class PreviewWarning(
    val kind: PreviewWarningKind,
    val count: Int?,
)

/** 문구가 없는 코드는 서버 전용 ROWS_OMITTED처럼 앱이 보여줄 일이 없으므로 뺀다. */
internal fun previewWarnings(warnings: List<ParseWarning>): List<PreviewWarning> =
    warnings
        .mapNotNull { warning -> warning.kind()?.let { kind -> PreviewWarning(kind, warning.count) } }
        .sortedBy { warning -> warning.kind.ordinal }

private fun ParseWarning.kind(): PreviewWarningKind? =
    when (code) {
        ParseWarningCode.ENCODING_FALLBACK_CP949 -> PreviewWarningKind.EncodingFallback
        ParseWarningCode.TEXT_INCONSISTENT_COLUMNS -> PreviewWarningKind.InconsistentColumns
        ParseWarningCode.XLSX_FORMULA_WITHOUT_VALUE -> PreviewWarningKind.FormulaWithoutValue
        ParseWarningCode.XLSX_NUMBER_FORMAT_RAW -> PreviewWarningKind.NumberFormatRaw
        ParseWarningCode.APKG_FIELD_COUNT_MISMATCH -> PreviewWarningKind.FieldCountMismatch
        ParseWarningCode.APKG_IMAGE_OCCLUSION_EXCLUDED -> PreviewWarningKind.ImageOcclusionExcluded
        else -> null
    }
