package com.whatever.caro.feature.importer.logic

import androidx.compose.ui.state.ToggleableState
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.transform.ImportPreview

internal const val DECK_PATH_SEPARATOR = "::"

internal data class SubdeckSelectionSummary(
    val selectedTotal: Int,
    val isOverLimit: Boolean,
)

internal sealed interface SheetRowLabel {
    data object Empty : SheetRowLabel

    data class Rows(
        val count: Int,
    ) : SheetRowLabel

    data object OverLimit : SheetRowLabel
}

internal enum class MappingBlockReason {
    NoTypeEnabled,
    NoFieldSelected,
    NoImportableType,
}

internal enum class PreviewBlockReason {
    NoCards,
    TooManyApkg,
    TooManyTextFile,
    TooManyPaste,
    TooManyXlsx,
}

internal enum class PasteBlockReason {
    Empty,
    TooLong,
}

/** 각 경로에서 마지막 이름을 뺀 부모 경로들의 공통 앞부분이다. 경로 전체의 공통 앞부분이면 단일 덱의 표시 이름이 비어 버린다. */
internal fun commonParentPath(paths: List<String>): String {
    val parents = paths.map { path -> path.split(DECK_PATH_SEPARATOR).dropLast(1) }
    val first = parents.firstOrNull() ?: return ""
    val commonLength = first.indices.takeWhile { index -> parents.all { parent -> parent.getOrNull(index) == first[index] } }.size
    return first.take(commonLength).joinToString(DECK_PATH_SEPARATOR)
}

internal fun subdeckDisplayName(
    path: String,
    commonParent: String,
): String {
    val remainder =
        when {
            commonParent.isEmpty() -> path
            path == commonParent -> ""
            else -> path.removePrefix(commonParent + DECK_PATH_SEPARATOR)
        }
    return remainder.ifEmpty { path.substringAfterLast(DECK_PATH_SEPARATOR) }
}

/** 카드가 없는 노트 수. 하위 덱은 카드가 있는 노트가 있는 덱만 담으므로 노트 유형 합계에서 뺀다. */
internal fun noDeckCount(table: ParsedTable): Int =
    (table.noteTypes.sumOf { type -> type.noteCount } - table.subdecks.sumOf { subdeck -> subdeck.noteCount }).coerceAtLeast(0)

internal fun summarizeSubdeckSelection(
    table: ParsedTable,
    selectedIds: Set<String>,
    maxCards: Int,
): SubdeckSelectionSummary {
    val subdeckTotal = table.subdecks.filter { subdeck -> subdeck.id in selectedIds }.sumOf { subdeck -> subdeck.noteCount }
    val noDeckTotal = if (ParsedSubdeck.NO_DECK_ID in selectedIds) noDeckCount(table) else 0
    val selectedTotal = subdeckTotal + noDeckTotal
    return SubdeckSelectionSummary(selectedTotal = selectedTotal, isOverLimit = selectedTotal > maxCards)
}

/** 덱 없음 묶음은 전체 선택의 대상이 아니므로 고른 id에 있어도 세지 않는다. */
internal fun selectAllState(
    table: ParsedTable,
    selectedIds: Set<String>,
): ToggleableState {
    val selectedCount = table.subdecks.count { subdeck -> subdeck.id in selectedIds }
    return when (selectedCount) {
        0 -> ToggleableState.Off
        table.subdecks.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
}

/** 세기 상한(`maxCards + 1`)에서 읽기가 멈추므로 상한보다 큰 값은 실제 행 수가 아니다. */
internal fun sheetRowLabel(
    sheet: ParsedSheet,
    maxCards: Int,
): SheetRowLabel =
    when {
        sheet.rowCount <= 0 -> SheetRowLabel.Empty
        sheet.rowCount > maxCards -> SheetRowLabel.OverLimit
        else -> SheetRowLabel.Rows(sheet.rowCount)
    }

/** 항목 연결 목록의 유형에서 Image Occlusion을 뺀다. */
internal fun importableNoteTypes(table: ParsedTable): List<ParsedNoteType> =
    mappingListNoteTypes(table).filterNot { type -> type.isImageOcclusion }

internal fun mappingBlockReason(
    table: ParsedTable,
    mappings: Map<String, FieldMapping>,
): MappingBlockReason? {
    val importable = importableNoteTypes(table)
    if (importable.isEmpty()) return MappingBlockReason.NoImportableType
    val enabled = importable.mapNotNull { type -> mappings[type.id]?.takeUnless { mapping -> mapping.excluded } }
    if (enabled.isEmpty()) return MappingBlockReason.NoTypeEnabled
    val hasField = enabled.any { mapping -> mapping.frontFields.isNotEmpty() || mapping.backFields.isNotEmpty() }
    return if (hasField) null else MappingBlockReason.NoFieldSelected
}

/** 상한 초과는 읽지 않은 행이 남았다는 뜻이라 카드가 0장이어도 나누라는 안내가 먼저다. */
internal fun previewBlockReason(
    preview: ImportPreview,
    format: ImportFormat,
    isPaste: Boolean,
): PreviewBlockReason? =
    when {
        preview.limitExceeded && isPaste -> {
            PreviewBlockReason.TooManyPaste
        }

        preview.limitExceeded -> {
            when (format) {
                ImportFormat.APKG -> PreviewBlockReason.TooManyApkg
                ImportFormat.TEXT -> PreviewBlockReason.TooManyTextFile
                ImportFormat.XLSX -> PreviewBlockReason.TooManyXlsx
            }
        }

        preview.cards.isEmpty() -> {
            PreviewBlockReason.NoCards
        }

        else -> {
            null
        }
    }

/** 상한을 넘어도 글을 자르지 않는다. 자르면 알리지 않은 채 카드가 사라진다. */
internal fun pasteBlockReason(
    text: String,
    maxPasteChars: Int,
): PasteBlockReason? =
    when {
        text.isEmpty() -> PasteBlockReason.Empty
        text.length > maxPasteChars -> PasteBlockReason.TooLong
        else -> null
    }
