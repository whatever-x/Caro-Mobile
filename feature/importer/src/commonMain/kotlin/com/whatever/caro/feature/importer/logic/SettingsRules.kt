package com.whatever.caro.feature.importer.logic

import androidx.compose.ui.state.ToggleableState
import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.mvi.SettingsKind
import com.whatever.caro.feature.importer.mvi.SettingsReason
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet

internal sealed interface SheetRowLabel {
    data object Empty : SheetRowLabel

    data class Rows(
        val count: Int,
    ) : SheetRowLabel

    data object OverLimit : SheetRowLabel
}

internal fun isSettingsKindAvailable(
    kind: SettingsKind,
    table: ParsedTable,
): Boolean =
    when (kind) {
        SettingsKind.Subdeck -> table.format == ImportFormat.APKG
        SettingsKind.Sheet -> table.format == ImportFormat.XLSX
        SettingsKind.Delimiter -> table.format == ImportFormat.TEXT
        SettingsKind.CardLayout -> importableNoteTypes(table).isNotEmpty()
    }

/** Image Occlusion 노트는 카드가 되지 않으므로 하위 덱 장수와 상한 판정에서 뺀다. 첫 읽기 판정도 같은 기준을 써야 하위 덱부터 고르게 한 조합을 그 화면에서 고를 수 있다. */
internal fun subdeckCardCount(subdeck: ParsedSubdeck): Int = subdeck.noteCount - subdeck.imageOcclusionNoteCount

/** 하위 덱은 한 덱 아래에 모여 있는 일이 많아, 공통 상위 경로를 그대로 두면 줄마다 같은 앞부분이 반복되고 이름이 잘린다. */
internal fun subdeckDisplayNames(table: ParsedTable): List<String> {
    val commonParent = commonParentPath(table.subdecks.map { subdeck -> subdeck.path })
    val prefix = if (commonParent.isEmpty()) "" else commonParent + DECK_PATH_SEPARATOR
    return table.subdecks.map { subdeck -> subdeck.path.removePrefix(prefix) }
}

internal fun selectedSubdeckCardCount(
    table: ParsedTable,
    subdeckIds: Set<String>,
): Int = table.subdecks.filter { subdeck -> subdeck.id in subdeckIds }.sumOf(::subdeckCardCount)

internal fun subdeckSelectAllState(
    table: ParsedTable,
    subdeckIds: Set<String>,
): ToggleableState {
    val selected = table.subdecks.count { subdeck -> subdeck.id in subdeckIds }
    return when (selected) {
        0 -> ToggleableState.Off
        table.subdecks.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
}

/** 모두 고른 상태에서만 모두 풀고, 일부만 고른 상태에서는 모두 고른다. */
internal fun toggledAllSubdecks(
    table: ParsedTable,
    subdeckIds: Set<String>,
): ImmutableSet<String> =
    if (subdeckSelectAllState(table, subdeckIds) == ToggleableState.On) {
        emptySet<String>().toImmutableSet()
    } else {
        table.subdecks.map { subdeck -> subdeck.id }.toImmutableSet()
    }

/** 시트 목록의 넘음 표시는 첫 읽기의 후보 시트와 같은 기준이다. 행 수는 제목 행을 포함한 값이다. */
internal fun sheetRowLabel(
    sheet: ParsedSheet,
    maxCards: Int,
): SheetRowLabel =
    when {
        sheet.rowCount <= 0 -> SheetRowLabel.Empty
        !isCandidateSheet(sheet, maxCards) -> SheetRowLabel.OverLimit
        else -> SheetRowLabel.Rows(sheet.rowCount)
    }

/** 시트를 읽지 않고 요약 행 수만으로 막는 사유다. 이 사유가 있는 시트는 다시 읽지 않는다. */
internal fun sheetSummaryReason(
    table: ParsedTable,
    sheetIndex: Int,
    maxCards: Int,
): SettingsReason? {
    val sheet = table.sheets.firstOrNull { candidate -> candidate.index == sheetIndex } ?: return SettingsReason.SheetEmpty
    return when (sheetRowLabel(sheet, maxCards)) {
        SheetRowLabel.Empty -> SettingsReason.SheetEmpty
        SheetRowLabel.OverLimit -> SettingsReason.SheetOverLimit
        is SheetRowLabel.Rows -> null
    }
}

/**
 * 하위 덱과 시트는 요약 값으로 미리 안다. 구분 기준과 카드 구성의 0장, 상한 초과는 카드를 만들어 봐야 알아 [appliedPreviewReason]이 정한다.
 * 시트는 요약을 통과해야 다시 읽으므로, 요약을 통과한 시트의 [table]은 그 시트를 읽은 결과다.
 */
internal fun draftSettingsReason(
    kind: SettingsKind,
    table: ParsedTable,
    selection: ImportSelection,
    mappings: Map<String, FieldMapping>,
    maxCards: Int,
): SettingsReason? =
    when (kind) {
        SettingsKind.Subdeck -> subdeckReason(table, selection.subdeckIds, maxCards)
        SettingsKind.Sheet -> sheetSummaryReason(table, selection.sheetIndex, maxCards) ?: readSheetReason(table, maxCards)
        SettingsKind.Delimiter -> null
        SettingsKind.CardLayout -> SettingsReason.NoTypeEnabled.takeIf { hasNoEnabledType(table, mappings) }
    }

/** 상한 초과는 읽지 않은 행이 남았다는 뜻이라 카드가 0장이어도 상한 사유가 먼저다. */
internal fun appliedPreviewReason(
    kind: SettingsKind,
    preview: ImportPreview,
): SettingsReason? =
    when {
        preview.limitExceeded -> overLimitReason(kind)
        preview.cards.isEmpty() -> SettingsReason.NoCards
        else -> null
    }

/** 항목 이름이 달라진 유형에 옛 카드 구성을 쓰면 없는 항목을 가리켜 빈 카드가 되므로, 항목 이름이 같은 유형만 고친 구성을 이어 쓴다. */
internal fun carryOverMappings(
    previous: ParsedTable,
    mappings: Map<String, FieldMapping>,
    next: ParsedTable,
): Map<String, FieldMapping> {
    val previousFields = previous.noteTypes.associate { type -> type.id to type.fieldNames }
    val nextFields = next.noteTypes.associate { type -> type.id to type.fieldNames }
    return defaultFieldMappings(next).mapValues { (id, default) ->
        val kept = mappings[id]
        if (kept != null && previousFields[id] == nextFields[id]) kept else default
    }
}

internal fun firstPreviewCards(
    table: ParsedTable,
    mappings: Map<String, FieldMapping>,
    emptySideText: String,
): ImmutableMap<String, ImportCard> =
    importableNoteTypes(table)
        .mapNotNull { type ->
            val mapping = mappings[type.id] ?: return@mapNotNull null
            firstPreviewCard(table, mapping, emptySideText)?.let { card -> type.id to card }
        }.toMap()
        .toImmutableMap()

/** 고른 항목이 모두 빈 유형은 첫 카드를 찾느라 행 끝까지 훑으므로, 카드 구성을 바꿀 때마다 모든 유형을 다시 만들면 큰 표에서 Main이 멈춘다. */
internal fun ImmutableMap<String, ImportCard>.withFirstPreviewCard(
    table: ParsedTable,
    mapping: FieldMapping,
    emptySideText: String,
): ImmutableMap<String, ImportCard> {
    val id = mapping.noteTypeId
    val card = if (importableNoteTypes(table).any { type -> type.id == id }) firstPreviewCard(table, mapping, emptySideText) else null
    val others = this - id
    return (if (card == null) others else others + (id to card)).toImmutableMap()
}

private fun subdeckReason(
    table: ParsedTable,
    subdeckIds: Set<String>,
    maxCards: Int,
): SettingsReason? {
    val hasSelected = table.subdecks.any { subdeck -> subdeck.id in subdeckIds }
    val count = selectedSubdeckCardCount(table, subdeckIds)
    return when {
        count > maxCards -> SettingsReason.SubdeckOverLimit
        hasSelected && count == 0 -> SettingsReason.NoCards
        else -> null
    }
}

private fun readSheetReason(
    table: ParsedTable,
    maxCards: Int,
): SettingsReason? = SettingsReason.SheetOverLimit.takeIf { table.rowLimitReached || table.rows.size > maxCards }

/** 카드 구성에 없는 유형은 미리보기 계산이 기본 구성으로 가져오므로 켜진 것으로 본다. */
private fun hasNoEnabledType(
    table: ParsedTable,
    mappings: Map<String, FieldMapping>,
): Boolean {
    val types = importableNoteTypes(table)
    return types.isNotEmpty() && types.all { type -> mappings[type.id]?.excluded == true }
}

private fun overLimitReason(kind: SettingsKind): SettingsReason =
    when (kind) {
        SettingsKind.Subdeck -> SettingsReason.SubdeckOverLimit
        SettingsKind.Sheet -> SettingsReason.SheetOverLimit
        SettingsKind.Delimiter, SettingsKind.CardLayout -> SettingsReason.TooManyCards
    }
