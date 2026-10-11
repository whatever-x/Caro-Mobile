package com.whatever.caro.feature.importer.logic

import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.model.importing.FieldMapping

/** 이 수를 넘는 유형은 칩 대신 고르기 시트를 쓴다. composable이 아니라 데이터 단계가 판단하도록 상수로 둔다. */
internal const val MAPPING_INLINE_FIELD_LIMIT = 12

private const val SUMMARY_NAME_LIMIT = 3

internal fun usesFieldPicker(type: ParsedNoteType): Boolean = type.fieldNames.size > MAPPING_INLINE_FIELD_LIMIT

internal data class ChosenFieldsSummary(
    val names: List<String>,
    val moreCount: Int,
)

internal fun summarizeChosenFields(chosen: List<String>): ChosenFieldsSummary =
    ChosenFieldsSummary(names = chosen.take(SUMMARY_NAME_LIMIT), moreCount = (chosen.size - SUMMARY_NAME_LIMIT).coerceAtLeast(0))

/** [order]는 이 면에 고른 순서(1부터)이고 고르지 않았으면 null이다. [isInOtherSide]는 반대 면에 놓인 항목이다. */
internal data class PickerRow(
    val name: String,
    val order: Int?,
    val isInOtherSide: Boolean,
)

/** 줄 순서는 번호가 아니라 원본 항목 순서다. 번호는 [target]에 고른 순서를 따른다. */
internal fun pickerRows(
    fieldNames: List<String>,
    mapping: FieldMapping,
    target: String,
): List<PickerRow> {
    val chosen = mapping.targets[target].orEmpty()
    val otherSide = mapping.targets[otherTarget(target)].orEmpty()
    return fieldNames.map { name ->
        val index = chosen.indexOf(name)
        PickerRow(name = name, order = if (index >= 0) index + 1 else null, isInOtherSide = name in otherSide)
    }
}

private fun otherTarget(target: String): String = if (target == FieldMapping.FRONT) FieldMapping.BACK else FieldMapping.FRONT
