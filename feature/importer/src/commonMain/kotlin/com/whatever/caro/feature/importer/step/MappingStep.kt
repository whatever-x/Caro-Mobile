package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_hint_mapping_merge
import caromobile.core.designsystem.generated.resources.import_subtitle_mapping
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.logic.mappingListNoteTypes
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.transform.ImportCard
import org.jetbrains.compose.resources.stringResource

private const val HEADER_KEY = "mapping-header"
private const val NOTE_TYPE = "type"
private const val IMAGE_OCCLUSION_TYPE = "image-occlusion"

/**
 * 유형마다 카드 하나를 목록 아이템으로 둔다. 가져올 수 없는 Image Occlusion은 연결 없이 이유만 보여 주고 목록 맨 아래에 둔다.
 * 유형 이름 머리는 Image Occlusion 묶음과 구분해야 하므로 묶음이 있으면 둔다.
 * 가져오기 체크는 가져올 수 있는 유형이 둘 이상일 때만 둔다. 하나뿐이면 끄는 순간 가져올 유형이 없다는 사유로 막힌다.
 */
internal fun LazyListScope.mappingItems(
    table: ParsedTable,
    mappings: Map<String, FieldMapping>,
    firstCards: Map<String, ImportCard>,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    val types = mappingListNoteTypes(table).sortedBy { type -> type.isImageOcclusion }
    val showsTypeHeader = types.size > 1
    val canToggleType = types.count { type -> !type.isImageOcclusion } > 1

    item(key = HEADER_KEY, contentType = HEADER_KEY) {
        SettingsHeader(
            fileName = null,
            title = stringResource(Res.string.import_subtitle_mapping),
            hint = stringResource(Res.string.import_hint_mapping_merge),
        )
    }
    itemsIndexed(
        types,
        key = { _, type -> "type-${type.id}" },
        contentType = { _, type -> if (type.isImageOcclusion) IMAGE_OCCLUSION_TYPE else NOTE_TYPE },
    ) { index, type ->
        val cardModifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2).padding(top = if (index == 0) 0.dp else CaroTheme.spacing.m)
        val mapping = mappings[type.id]
        when {
            type.isImageOcclusion -> {
                ImageOcclusionCard(type = type, modifier = cardModifier)
            }

            mapping != null -> {
                NoteTypeCard(
                    type = type,
                    mapping = mapping,
                    firstCard = firstCards[type.id],
                    showsHeader = showsTypeHeader,
                    onToggleNoteType = { id: String -> onIntent(ImportDeckIntent.ToggleNoteType(id)) }.takeIf { canToggleType },
                    onToggleField = { id, target, fieldName -> onIntent(ImportDeckIntent.ToggleField(id, target, fieldName)) },
                    modifier = cardModifier,
                )
            }
        }
    }
}
