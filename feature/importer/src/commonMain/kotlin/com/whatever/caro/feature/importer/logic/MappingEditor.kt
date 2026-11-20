package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping

/**
 * 이미 [target]에 고른 항목이면 선택을 풀고, 아니면 [target]의 맨 뒤에 넣는다.
 * 한 항목은 한 면에만 놓이므로 다른 면에 있던 항목은 그 면에서 빼고 뒤 번호를 당긴다.
 */
internal fun toggleField(
    mapping: FieldMapping,
    target: String,
    fieldName: String,
): FieldMapping {
    val chosen = mapping.targets[target].orEmpty()
    if (fieldName in chosen) {
        return mapping.copy(targets = mapping.targets + (target to (chosen - fieldName)))
    }
    val withoutField = mapping.targets.mapValues { (_, names) -> names - fieldName }
    return mapping.copy(targets = withoutField + (target to (chosen + fieldName)))
}

/** 고른 범위에 노트가 있는 유형이다. Image Occlusion도 안내 카드를 보여주므로 포함한다. 고른 범위를 모르면(null) 전체 노트 수로 본다. */
internal fun mappingListNoteTypes(table: ParsedTable): List<ParsedNoteType> =
    table.noteTypes.filter { type -> (type.selectedNoteCount ?: type.noteCount) > 0 }
