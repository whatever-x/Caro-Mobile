package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ImportFormat
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.transform.ImportCard
import com.whatever.caro.feature.importer.transform.ImportPreview
import com.whatever.caro.feature.importer.transform.ImportPreviewBuilder

enum class NoCardsKind {
    ImageOcclusionOnly,
    NoContent,
}

/** Image Occlusion 말고 다른 노트가 하나라도 있으면 카드 구성을 바꿔 고칠 수 있으므로 [NoCardsKind.NoContent]다. */
internal fun noCardsKind(
    table: ParsedTable,
    preview: ImportPreview,
): NoCardsKind? {
    if (preview.cards.isNotEmpty()) return null
    val types = mappingListNoteTypes(table)
    return if (types.isNotEmpty() && types.all { type -> type.isImageOcclusion }) NoCardsKind.ImageOcclusionOnly else NoCardsKind.NoContent
}

/** 붙여넣기도 텍스트 형식이다. 엑셀은 칸이 나뉘어 있어 구분 기준을 고칠 일이 없다. */
internal fun needsDelimiterCheck(table: ParsedTable): Boolean {
    val dataRows = table.rows.size
    val singleFieldRows = table.layout?.singleFieldRowCount ?: 0
    return table.format == ImportFormat.TEXT && dataRows > 0 && singleFieldRows * 2 >= dataRows
}

/** 미리보기와 같은 규칙으로 카드를 만들어야 첫 카드가 실제로 저장될 카드와 같다. 가져오기를 끈 유형과 Image Occlusion은 카드가 없다. */
internal fun firstPreviewCard(
    table: ParsedTable,
    mapping: FieldMapping,
    emptySideText: String,
): ImportCard? {
    val type = table.noteTypes.firstOrNull { candidate -> candidate.id == mapping.noteTypeId }
    if (type == null || type.isImageOcclusion || mapping.excluded) return null
    val builder = ImportPreviewBuilder()
    return table.rows.withIndex().firstNotNullOfOrNull { (index, row) ->
        if (row.noteTypeId == type.id) builder.buildCard(index, row, table, type.isCloze, mapping, emptySideText) else null
    }
}
