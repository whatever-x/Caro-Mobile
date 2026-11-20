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
import caromobile.core.designsystem.generated.resources.import_content_description_image_occlusion
import caromobile.core.designsystem.generated.resources.import_hint_cloze_banner
import caromobile.core.designsystem.generated.resources.import_hint_image_occlusion
import caromobile.core.designsystem.generated.resources.import_label_back_fields
import caromobile.core.designsystem.generated.resources.import_label_card_count
import caromobile.core.designsystem.generated.resources.import_label_cloze_banner
import caromobile.core.designsystem.generated.resources.import_label_cloze_extra_fields
import caromobile.core.designsystem.generated.resources.import_label_cloze_fields
import caromobile.core.designsystem.generated.resources.import_label_front_fields
import caromobile.core.designsystem.generated.resources.import_label_import_toggle
import caromobile.core.designsystem.generated.resources.import_label_not_importable
import caromobile.core.designsystem.generated.resources.import_label_side_back
import caromobile.core.designsystem.generated.resources.import_label_side_cloze
import caromobile.core.designsystem.generated.resources.import_label_side_extra
import caromobile.core.designsystem.generated.resources.import_label_side_front
import caromobile.core.designsystem.generated.resources.import_tag_field_count
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.components.ImportCheckbox
import com.whatever.caro.feature.importer.components.ImportInfoTag
import com.whatever.caro.feature.importer.components.isLargeText
import com.whatever.caro.feature.importer.logic.truncateNoteTypeName
import com.whatever.caro.feature.importer.transform.ImportCard
import org.jetbrains.compose.resources.stringResource

private val CardBorderWidth = 1.dp
private val ToggleMinHeight = 48.dp
private val ToggleGap = 6.dp
private const val DISABLED_ALPHA = 0.4f
private const val NAME_MAX_LINES = 2

/**
 * 가져오지 않는 유형은 칩을 [DISABLED_ALPHA]로 흐리게 그려 누를 수 없게 하고 첫 카드 미리보기를 숨긴다. 머리 줄은 다시 켤 수 있도록 흐리게 하지 않는다.
 * 첫 카드는 ViewModel이 내용 있는 카드를 골라 두므로 없으면 만들 카드가 없는 것이다.
 * [onToggleNoteType]이 null이면 머리 줄에 가져오기 체크 없이 이름과 태그만 둔다.
 */
@Composable
internal fun NoteTypeCard(
    type: ParsedNoteType,
    mapping: FieldMapping,
    firstCard: ImportCard?,
    showsHeader: Boolean,
    onToggleNoteType: ((String) -> Unit)?,
    onToggleField: (noteTypeId: String, target: String, fieldName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEnabled = !mapping.excluded

    Column(
        modifier = modifier.cardSurface(CaroTheme.color.surface.primary),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        if (showsHeader) NoteTypeHeader(type = type, isEnabled = isEnabled, onToggleNoteType = onToggleNoteType)
        Column(
            modifier = Modifier.alpha(if (isEnabled) 1f else DISABLED_ALPHA),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        ) {
            if (type.isCloze) ClozeBanner()
            FieldSides(type = type, mapping = mapping, isEnabled = isEnabled, onToggleField = onToggleField)
        }
        if (isEnabled && firstCard != null) FirstCardSection(card = firstCard, isLoading = false)
    }
}

@Composable
private fun FieldSides(
    type: ParsedNoteType,
    mapping: FieldMapping,
    isEnabled: Boolean,
    onToggleField: (noteTypeId: String, target: String, fieldName: String) -> Unit,
) {
    val isCloze = type.isCloze
    val frontSide = stringResource(if (isCloze) Res.string.import_label_side_cloze else Res.string.import_label_side_front)
    val backSide = stringResource(if (isCloze) Res.string.import_label_side_extra else Res.string.import_label_side_back)
    val frontHeading = stringResource(if (isCloze) Res.string.import_label_cloze_fields else Res.string.import_label_front_fields)
    val backHeading = stringResource(if (isCloze) Res.string.import_label_cloze_extra_fields else Res.string.import_label_back_fields)

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

/** 하위 semantics를 지워 이름과 장수와 가져올 수 없다는 상태를 한 문장으로 읽게 한다. */
@Composable
internal fun ImageOcclusionCard(
    type: ParsedNoteType,
    modifier: Modifier = Modifier,
) {
    val name = truncateNoteTypeName(type.name)
    val count = type.shownCardCount().formatWithComma()
    val description = stringResource(Res.string.import_content_description_image_occlusion, name, count)

    Column(
        modifier = modifier.cardSurface(CaroTheme.color.surface.primary).clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
            NoteTypeInfo(
                name = name,
                tags = listOf(stringResource(Res.string.import_label_card_count, count)),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(Res.string.import_label_not_importable),
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
            )
        }
        Text(
            text = stringResource(Res.string.import_hint_image_occlusion),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(CaroTheme.shape.s)
                    .background(CaroTheme.color.surface.complete)
                    .padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
            style = CaroTheme.typography.body2.medium,
            color = CaroTheme.color.text.secondary,
        )
    }
}

/** 테두리는 안쪽 여백 바깥에 그리므로 여백에 테두리 두께를 더해 내용이 테두리에 붙지 않게 한다. */
@Composable
private fun Modifier.cardSurface(fill: Color): Modifier {
    val shape = CaroTheme.shape.m
    return fillMaxWidth()
        .clip(shape)
        .background(fill)
        .border(CardBorderWidth, CaroTheme.color.border.secondary, shape)
        .padding(CardBorderWidth)
        .padding(CaroTheme.spacing.xl)
}

/**
 * 머리 줄 어디를 눌러도 가져오기를 켜고 끄고, 이름과 태그가 함께 읽혀 어느 유형의 토글인지 알 수 있다.
 * 큰 글자에서는 이름과 태그가 가져오기 표시에 밀려 글자 단위로 끊기므로 표시를 아래로 내린다.
 */
@Composable
private fun NoteTypeHeader(
    type: ParsedNoteType,
    isEnabled: Boolean,
    onToggleNoteType: ((String) -> Unit)?,
) {
    val name = truncateNoteTypeName(type.name)
    val tags =
        listOf(
            stringResource(Res.string.import_label_card_count, type.shownCardCount().formatWithComma()),
            stringResource(Res.string.import_tag_field_count, type.fieldNames.size.formatWithComma()),
        )
    if (onToggleNoteType == null) {
        NoteTypeInfo(name = name, tags = tags)
        return
    }
    val toggle =
        Modifier
            .fillMaxWidth()
            .heightIn(min = ToggleMinHeight)
            .toggleable(value = isEnabled, role = Role.Checkbox, onValueChange = { onToggleNoteType(type.id) })

    if (isLargeText()) {
        Column(modifier = toggle, verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
            NoteTypeInfo(name = name, tags = tags)
            ImportToggleMark(isEnabled = isEnabled)
        }
    } else {
        Row(
            modifier = toggle,
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoteTypeInfo(name = name, tags = tags, modifier = Modifier.weight(1f))
            ImportToggleMark(isEnabled = isEnabled)
        }
    }
}

@Composable
private fun NoteTypeInfo(
    name: String,
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs)) {
        Text(
            text = name,
            style = CaroTheme.typography.heading2,
            color = CaroTheme.color.text.primary,
            maxLines = NAME_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs)) {
            tags.forEach { tag -> ImportInfoTag(text = tag) }
        }
    }
}

/** 상태 semantics는 머리 줄의 toggleable이 맡으므로 이 표시는 장식이다. */
@Composable
private fun ImportToggleMark(isEnabled: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(ToggleGap), verticalAlignment = Alignment.CenterVertically) {
        ImportCheckbox(state = if (isEnabled) ToggleableState.On else ToggleableState.Off)
        Text(
            text = stringResource(Res.string.import_label_import_toggle),
            style = CaroTheme.typography.body2.medium,
            color = CaroTheme.color.text.primary,
        )
    }
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

/** 고른 범위를 모르면(null) 전체 수로 본다. Image Occlusion은 설정 박스의 제외 장수와 같아야 하므로 노트가 아니라 카드 수다. */
private fun ParsedNoteType.shownCardCount(): Int {
    val notes = selectedNoteCount ?: noteCount
    return if (isImageOcclusion) selectedCardCount ?: notes else notes
}
