package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_button_preview
import caromobile.core.designsystem.generated.resources.import_hint_mapping_merge
import caromobile.core.designsystem.generated.resources.import_reason_mapping_no_field
import caromobile.core.designsystem.generated.resources.import_reason_mapping_no_type
import caromobile.core.designsystem.generated.resources.import_reason_mapping_nothing_importable
import caromobile.core.designsystem.generated.resources.import_subtitle_mapping
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.component.ImportStepTitle
import com.whatever.caro.feature.importer.logic.MappingBlockReason
import com.whatever.caro.feature.importer.logic.mappingBlockReason
import com.whatever.caro.feature.importer.logic.mappingListNoteTypes
import kotlinx.collections.immutable.PersistentMap
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val HEADER_KEY = "header"
private const val FOOTER_KEY = "footer"

/**
 * 유형마다 카드 하나를 LazyColumn 아이템으로 둬 세로 스크롤 컨테이너가 하나다.
 * 가져올 수 없는 Image Occlusion은 연결 없이 이유만 보여주고 목록 맨 위에 둔다.
 * [onToggleField]의 target은 `FieldMapping.FRONT` 또는 `FieldMapping.BACK`이다.
 */
@Composable
internal fun MappingStep(
    table: ParsedTable,
    mappings: PersistentMap<String, FieldMapping>,
    onToggleNoteType: (String) -> Unit,
    onToggleField: (noteTypeId: String, target: String, fieldName: String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val noteTypes = remember(table) { mappingListNoteTypes(table).sortedByDescending { type -> type.isImageOcclusion } }
    val blockReason = mappingBlockReason(table, mappings)
    val reason = blockReason?.let { stringResource(it.resource()) }

    SelectionStepFrame(
        buttonText = stringResource(Res.string.import_button_preview),
        onNext = onNext,
        enabled = blockReason == null,
        reason = reason,
        modifier = modifier,
    ) {
        item(key = HEADER_KEY) { MappingHeader() }
        items(noteTypes, key = { type -> "type-${type.id}" }) { type ->
            val cardModifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2).padding(bottom = CaroTheme.spacing.m)
            if (type.isImageOcclusion) {
                ImageOcclusionCard(type = type, modifier = cardModifier)
            } else {
                NoteTypeCard(
                    type = type,
                    // 연결이 없는 유형은 가져오지 않는 유형과 같게 보인다.
                    mapping = mappings[type.id] ?: FieldMapping(noteTypeId = type.id, targets = emptyMap(), excluded = true),
                    onToggleNoteType = onToggleNoteType,
                    onToggleField = onToggleField,
                    modifier = cardModifier,
                )
            }
        }
        item(key = FOOTER_KEY) { Spacer(modifier = Modifier.height(CaroTheme.spacing.m)) }
    }
}

/** 제목은 [ImportStepTitle]이어야 읽기가 끝난 뒤 제목으로 포커스가 간다. */
@Composable
private fun MappingHeader() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = CaroTheme.spacing.xl2,
                    top = CaroTheme.spacing.xl,
                    end = CaroTheme.spacing.xl2,
                    bottom = CaroTheme.spacing.xl2,
                ),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        ImportStepTitle(text = stringResource(Res.string.import_subtitle_mapping))
        Text(
            text = stringResource(Res.string.import_hint_mapping_merge),
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.secondary,
        )
    }
}

private fun MappingBlockReason.resource(): StringResource =
    when (this) {
        MappingBlockReason.NoTypeEnabled -> Res.string.import_reason_mapping_no_type
        MappingBlockReason.NoFieldSelected -> Res.string.import_reason_mapping_no_field
        MappingBlockReason.NoImportableType -> Res.string.import_reason_mapping_nothing_importable
    }
