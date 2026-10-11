package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_a11y_image_occlusion
import caromobile.core.designsystem.generated.resources.import_hint_cloze_banner
import caromobile.core.designsystem.generated.resources.import_hint_image_occlusion
import caromobile.core.designsystem.generated.resources.import_label_back_fields
import caromobile.core.designsystem.generated.resources.import_label_cloze_banner
import caromobile.core.designsystem.generated.resources.import_label_cloze_extra_fields
import caromobile.core.designsystem.generated.resources.import_label_cloze_fields
import caromobile.core.designsystem.generated.resources.import_label_front_fields
import caromobile.core.designsystem.generated.resources.import_label_import_toggle
import caromobile.core.designsystem.generated.resources.import_label_note_type_count
import caromobile.core.designsystem.generated.resources.import_label_note_type_summary
import caromobile.core.designsystem.generated.resources.import_label_side_back
import caromobile.core.designsystem.generated.resources.import_label_side_cloze
import caromobile.core.designsystem.generated.resources.import_label_side_extra
import caromobile.core.designsystem.generated.resources.import_label_side_front
import com.whatever.caro.core.designsystem.components.CaroCheckbox
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.logic.truncateNoteTypeName
import org.jetbrains.compose.resources.stringResource

private val CardBorderWidth = 1.dp
private val ToggleMinHeight = 48.dp
private val ToggleGap = 6.dp
private const val DISABLED_ALPHA = 0.4f
private const val NAME_MAX_LINES = 2

private fun ParsedNoteType.shownNoteCount(): Int = selectedNoteCount ?: noteCount

/** 가져오지 않는 유형은 [DISABLED_ALPHA]로 흐리게 그리고 칩을 누를 수 없게 한다. 가져오기 줄은 다시 켤 수 있도록 흐리게 하지 않는다. */
@Composable
internal fun NoteTypeCard(
    type: ParsedNoteType,
    mapping: FieldMapping,
    onToggleNoteType: (String) -> Unit,
    onToggleField: (noteTypeId: String, target: String, fieldName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEnabled = !mapping.excluded
    val isCloze = type.isCloze

    Column(
        modifier = modifier.cardSurface(CaroTheme.color.surface.primary, CaroTheme.color.border.secondary),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        NoteTypeHeader(type = type, isEnabled = isEnabled, onToggleNoteType = onToggleNoteType)
        Column(
            modifier = Modifier.alpha(if (isEnabled) 1f else DISABLED_ALPHA),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        ) {
            if (isCloze) ClozeBanner()
            val frontSide = stringResource(if (isCloze) Res.string.import_label_side_cloze else Res.string.import_label_side_front)
            val backSide = stringResource(if (isCloze) Res.string.import_label_side_extra else Res.string.import_label_side_back)
            val frontHeading = stringResource(if (isCloze) Res.string.import_label_cloze_fields else Res.string.import_label_front_fields)
            val backHeading =
                stringResource(if (isCloze) Res.string.import_label_cloze_extra_fields else Res.string.import_label_back_fields)
            FieldSide(
                type = type,
                mapping = mapping,
                side = FieldSideSpec(FieldMapping.FRONT, heading = frontHeading, name = frontSide, otherName = backSide),
                isEnabled = isEnabled,
                onToggleField = onToggleField,
            )
            FieldSide(
                type = type,
                mapping = mapping,
                side = FieldSideSpec(FieldMapping.BACK, heading = backHeading, name = backSide, otherName = frontSide),
                isEnabled = isEnabled,
                onToggleField = onToggleField,
            )
        }
    }
}

/** 하위 semantics를 지워 이름과 노트 수와 이유를 한 문장으로 읽게 한다. */
@Composable
internal fun ImageOcclusionCard(
    type: ParsedNoteType,
    modifier: Modifier = Modifier,
) {
    val name = truncateNoteTypeName(type.name)
    val count = type.shownNoteCount().formatWithComma()
    val description = stringResource(Res.string.import_a11y_image_occlusion, name, count)

    Column(
        modifier =
            modifier
                .cardSurface(CaroTheme.color.surface.secondary, CaroTheme.color.border.ready)
                .clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs)) {
            NoteTypeName(name)
            Text(
                text = stringResource(Res.string.import_label_note_type_count, count),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.secondary,
            )
        }
        Text(
            text = stringResource(Res.string.import_hint_image_occlusion),
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.secondary,
        )
    }
}

/** 테두리는 안쪽 여백 바깥에 그리므로 여백에 테두리 두께를 더해 내용이 테두리에 붙지 않게 한다. */
@Composable
private fun Modifier.cardSurface(
    fill: Color,
    line: Color,
): Modifier {
    val shape = CaroTheme.shape.m
    return fillMaxWidth()
        .clip(shape)
        .background(fill)
        .border(CardBorderWidth, line, shape)
        .padding(CardBorderWidth)
        .padding(start = CaroTheme.spacing.xl, top = CaroTheme.spacing.m, end = CaroTheme.spacing.xl, bottom = CaroTheme.spacing.xl)
}

/** 큰 글자에서는 이름과 부제가 가져오기 줄에 밀려 글자 단위로 끊기므로 가져오기 줄을 아래로 내린다. */
@Composable
private fun NoteTypeHeader(
    type: ParsedNoteType,
    isEnabled: Boolean,
    onToggleNoteType: (String) -> Unit,
) {
    if (isLargeText()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            NoteTypeInfo(type)
            ImportToggle(isEnabled = isEnabled, onClick = { onToggleNoteType(type.id) })
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoteTypeInfo(type, modifier = Modifier.weight(1f))
            ImportToggle(isEnabled = isEnabled, onClick = { onToggleNoteType(type.id) })
        }
    }
}

@Composable
private fun NoteTypeInfo(
    type: ParsedNoteType,
    modifier: Modifier = Modifier,
) {
    val count = type.shownNoteCount().formatWithComma()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs)) {
        NoteTypeName(truncateNoteTypeName(type.name))
        Text(
            text = stringResource(Res.string.import_label_note_type_summary, count, type.fieldNames.size.formatWithComma()),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
    }
}

@Composable
private fun ImportToggle(
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .heightIn(min = ToggleMinHeight)
                .toggleable(value = isEnabled, role = Role.Checkbox, onValueChange = { onClick() }),
        horizontalArrangement = Arrangement.spacedBy(ToggleGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CaroCheckbox(state = if (isEnabled) ToggleableState.On else ToggleableState.Off)
        Text(
            text = stringResource(Res.string.import_label_import_toggle),
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.primary,
        )
    }
}

@Composable
private fun NoteTypeName(text: String) {
    Text(
        text = text,
        style = CaroTheme.typography.body1,
        color = CaroTheme.color.text.primary,
        maxLines = NAME_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ClozeBanner() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CaroTheme.shape.s)
                .background(CaroTheme.color.surface.secondary)
                .padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.import_label_cloze_banner),
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.brand,
        )
        Text(
            text = stringResource(Res.string.import_hint_cloze_banner),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
    }
}
