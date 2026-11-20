package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParseWarningCode
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.defaultFieldMappings
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private const val MIN_SUBDECKS_TO_MERGE = 2

internal enum class SettingRowKind {
    Subdeck,
    Sheet,
    Delimiter,
    CardLayout,
    Excluded,
}

internal data class SettingRow(
    val kind: SettingRowKind,
    val tags: ImmutableList<SettingTag>,
)

internal sealed interface SettingTag {
    data class SubdeckAll(
        val count: Int,
    ) : SettingTag

    data class SubdeckSome(
        val total: Int,
        val selected: Int,
    ) : SettingTag

    data object SubdeckMerged : SettingTag

    data class NoteTypeCount(
        val count: Int,
    ) : SettingTag

    data object SidesAuto : SettingTag

    data object SidesManual : SettingTag

    data class SheetName(
        val name: String,
    ) : SettingTag

    data class HeaderRow(
        val isOn: Boolean,
    ) : SettingTag

    /** [kind]가 null이면 이름이 없는 문자라 [text] 자체가 라벨이다. */
    data class Delimiter(
        val kind: DelimiterKind?,
        val text: String,
    ) : SettingTag

    data class FrontFields(
        val names: ImmutableList<TextCheckHeader>,
        val moreCount: Int,
    ) : SettingTag

    data class BackFields(
        val names: ImmutableList<TextCheckHeader>,
        val moreCount: Int,
    ) : SettingTag

    data object BackNone : SettingTag

    data class EmptyCards(
        val count: Int,
    ) : SettingTag

    data class ImageOcclusionCards(
        val count: Int,
    ) : SettingTag

    data class OtherSheets(
        val count: Int,
    ) : SettingTag
}

/** 덱 이름 줄은 상태의 덱 이름을 그대로 보이므로 여기서 만들지 않는다. 값이 0인 태그와 태그가 없는 줄은 뺀다. */
internal fun previewSettingRows(
    table: ParsedTable,
    selection: ImportSelection,
    mappings: Map<String, FieldMapping>,
    preview: ImportPreview,
): List<SettingRow> =
    listOf(
        sourceRow(table, selection),
        settingRow(SettingRowKind.CardLayout, cardLayoutTags(table, mappings)),
        settingRow(SettingRowKind.Excluded, excludedTags(table, preview)),
    ).filter { row -> row.tags.isNotEmpty() }

private fun settingRow(
    kind: SettingRowKind,
    tags: List<SettingTag>,
): SettingRow = SettingRow(kind, tags.toImmutableList())

private fun sourceRow(
    table: ParsedTable,
    selection: ImportSelection,
): SettingRow =
    when (table.format) {
        ImportFormat.APKG -> settingRow(SettingRowKind.Subdeck, subdeckTags(table, selection))
        ImportFormat.XLSX -> settingRow(SettingRowKind.Sheet, listOfNotNull(sheetTag(table, selection), headerRowTag(table)))
        ImportFormat.TEXT -> settingRow(SettingRowKind.Delimiter, listOfNotNull(delimiterTag(table), headerRowTag(table)))
    }

/** 하위 덱이 하나면 합칠 덱이 없으므로 합침 태그를 두지 않는다. */
private fun subdeckTags(
    table: ParsedTable,
    selection: ImportSelection,
): List<SettingTag> {
    val total = table.subdecks.size
    val selected = table.subdecks.count { subdeck -> subdeck.id in selection.subdeckIds }
    return listOfNotNull(
        SettingTag.SubdeckAll(selected).takeIf { selected > 0 && selected == total },
        SettingTag.SubdeckSome(total, selected).takeIf { selected in 1 until total },
        SettingTag.SubdeckMerged.takeIf { selected >= MIN_SUBDECKS_TO_MERGE },
    )
}

private fun sheetTag(
    table: ParsedTable,
    selection: ImportSelection,
): SettingTag? =
    table.sheets
        .firstOrNull { sheet -> sheet.index == selection.sheetIndex }
        ?.let { sheet -> SettingTag.SheetName(truncateSheetName(sheet.name)) }

private fun headerRowTag(table: ParsedTable): SettingTag? = table.layout?.let { layout -> SettingTag.HeaderRow(layout.hasHeaderRow) }

private fun delimiterTag(table: ParsedTable): SettingTag? =
    table.layout
        ?.delimiter
        ?.takeIf { delimiter -> delimiter.isNotEmpty() }
        ?.let { delimiter -> SettingTag.Delimiter(delimiter.singleOrNull()?.let(::delimiterKind), delimiter) }

/**
 * Anki는 유형마다 항목이 달라 항목 이름 대신 가져오는 유형 수를 보인다. 텍스트와 엑셀은 유형이 하나다.
 * 직접 지정은 가져오는 유형의 앞뒷면 항목이 기본과 다를 때다. 유형을 끈 것은 유형 수 태그가 이미 보이므로 앞뒷면 태그에 넣지 않는다.
 */
private fun cardLayoutTags(
    table: ParsedTable,
    mappings: Map<String, FieldMapping>,
): List<SettingTag> {
    val types = importableNoteTypes(table)
    if (table.format == ImportFormat.APKG) {
        if (types.isEmpty()) return emptyList()
        val defaults = defaultFieldMappings(table)
        val enabled = types.filter { type -> mappings[type.id]?.excluded != true }
        val isManual = enabled.any { type -> (mappings[type.id] ?: defaults[type.id])?.targets != defaults[type.id]?.targets }
        return listOf(SettingTag.NoteTypeCount(enabled.size), if (isManual) SettingTag.SidesManual else SettingTag.SidesAuto)
    }
    val type = types.firstOrNull() ?: return emptyList()
    val mapping = mappings[type.id] ?: return emptyList()
    val front = mapping.frontFields.takeIf { names -> names.isNotEmpty() }?.let { names -> fieldsTag(type, names, isFront = true) }
    val back = mapping.backFields.takeIf { names -> names.isNotEmpty() }?.let { names -> fieldsTag(type, names, isFront = false) }
    return listOfNotNull(front, back ?: SettingTag.BackNone)
}

private fun fieldsTag(
    type: ParsedNoteType,
    chosen: List<String>,
    isFront: Boolean,
): SettingTag {
    val summary = summarizeChosenFields(chosen)
    val names =
        summary.names
            .map { name -> headerOf(name, number = type.fieldNames.indexOf(name) + 1, isDuplicated = false) }
            .toImmutableList()
    return if (isFront) SettingTag.FrontFields(names, summary.moreCount) else SettingTag.BackFields(names, summary.moreCount)
}

private fun excludedTags(
    table: ParsedTable,
    preview: ImportPreview,
): List<SettingTag> {
    val imageOcclusionCards =
        table.warnings.firstOrNull { warning -> warning.code == ParseWarningCode.APKG_IMAGE_OCCLUSION_EXCLUDED }?.count ?: 0
    val otherSheets = if (table.format == ImportFormat.XLSX) table.sheets.size - 1 else 0
    return listOfNotNull(
        SettingTag.EmptyCards(preview.stats.excludedCount).takeIf { preview.stats.excludedCount > 0 },
        SettingTag.ImageOcclusionCards(imageOcclusionCards).takeIf { imageOcclusionCards > 0 },
        SettingTag.OtherSheets(otherSheets).takeIf { otherSheets > 0 },
    )
}
