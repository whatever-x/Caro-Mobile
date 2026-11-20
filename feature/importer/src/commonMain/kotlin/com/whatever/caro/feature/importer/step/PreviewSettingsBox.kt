package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_button_change
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_colon
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_comma
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_pipe
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_semicolon
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_space
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_tab
import caromobile.core.designsystem.generated.resources.import_content_description_change_setting
import caromobile.core.designsystem.generated.resources.import_label_column
import caromobile.core.designsystem.generated.resources.import_label_delimiter
import caromobile.core.designsystem.generated.resources.import_label_fields_more
import caromobile.core.designsystem.generated.resources.import_label_setting_card_layout
import caromobile.core.designsystem.generated.resources.import_label_setting_deck_name
import caromobile.core.designsystem.generated.resources.import_label_setting_excluded
import caromobile.core.designsystem.generated.resources.import_label_setting_sheet
import caromobile.core.designsystem.generated.resources.import_label_setting_subdeck
import caromobile.core.designsystem.generated.resources.import_tag_back_fields
import caromobile.core.designsystem.generated.resources.import_tag_back_none
import caromobile.core.designsystem.generated.resources.import_tag_excluded_empty_cards
import caromobile.core.designsystem.generated.resources.import_tag_excluded_image_occlusion
import caromobile.core.designsystem.generated.resources.import_tag_excluded_other_sheets
import caromobile.core.designsystem.generated.resources.import_tag_front_fields
import caromobile.core.designsystem.generated.resources.import_tag_header_row_off
import caromobile.core.designsystem.generated.resources.import_tag_header_row_on
import caromobile.core.designsystem.generated.resources.import_tag_note_type_count
import caromobile.core.designsystem.generated.resources.import_tag_sides_auto
import caromobile.core.designsystem.generated.resources.import_tag_sides_manual
import caromobile.core.designsystem.generated.resources.import_tag_subdeck_all
import caromobile.core.designsystem.generated.resources.import_tag_subdeck_merged
import caromobile.core.designsystem.generated.resources.import_tag_subdeck_some
import caromobile.core.designsystem.generated.resources.import_title_settings
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.components.ImportInfoTag
import com.whatever.caro.feature.importer.components.isLargeText
import com.whatever.caro.feature.importer.logic.DelimiterKind
import com.whatever.caro.feature.importer.logic.SettingRow
import com.whatever.caro.feature.importer.logic.SettingRowKind
import com.whatever.caro.feature.importer.logic.SettingTag
import com.whatever.caro.feature.importer.logic.TextCheckHeader
import com.whatever.caro.feature.importer.mvi.SettingsKind
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val FIELD_SEPARATOR = ", "
private const val TAG_MAX_LINES = 2
private val BorderWidth = 1.dp
private val LabelWidth = 77.dp
private val RowContentMinHeight = 22.dp

// 줄 높이 48은 [변경] 버튼의 누르는 높이에서 나왔다. 버튼이 없어도 줄 간격은 같게 둔다.
private val RowVerticalPadding = 13.dp

// 보이는 [변경](약 29)의 좌우로 이만큼 넓혀야 누르는 폭이 48을 넘는다. 오른쪽으로 넓힌 만큼은 설정 박스의 안쪽 여백 안에 든다.
private val ChangeTouchSideExtension = 12.dp

/** 큰 글자에서는 값이 줄 이름 옆에 들어가지 않으므로 줄 이름 아래로 내린다. */
@Composable
internal fun PreviewSettingsBox(
    deckName: String,
    rows: ImmutableList<SettingRow>,
    onChange: (SettingsKind) -> Unit,
    onChangeDeckInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLarge = isLargeText()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(CaroTheme.color.surface.primary, CaroTheme.shape.m)
                .border(BorderWidth, CaroTheme.color.border.secondary, CaroTheme.shape.m)
                .padding(BorderWidth)
                .padding(CaroTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        Text(
            text = stringResource(Res.string.import_title_settings),
            modifier = Modifier.semantics { heading() },
            style = CaroTheme.typography.body1,
            color = CaroTheme.color.text.primary,
        )
        Box(modifier = Modifier.fillMaxWidth().height(BorderWidth).background(CaroTheme.color.divider.secondary))
        Column(verticalArrangement = Arrangement.spacedBy(if (isLarge) CaroTheme.spacing.s else 0.dp)) {
            SettingLine(
                stringResource(Res.string.import_label_setting_deck_name),
                isLarge,
                isLast = rows.isEmpty(),
                onChange = onChangeDeckInfo,
            ) {
                DeckNameValue(deckName)
            }
            rows.forEachIndexed { index, row ->
                val change = row.kind.settingsKind()?.let { kind -> { onChange(kind) } }
                SettingLine(stringResource(row.kind.label()), isLarge, isLast = index == rows.lastIndex, onChange = change) {
                    SettingTags(row.tags)
                }
            }
        }
    }
}

/** 줄 이름과 값을 한 문장으로 읽게 묶는다. [변경]은 clickable이 자기 노드로 따로 묶이므로 줄 문장에 섞이지 않는다. */
@Composable
private fun SettingLine(
    label: String,
    isLarge: Boolean,
    isLast: Boolean,
    onChange: (() -> Unit)?,
    value: @Composable () -> Unit,
) {
    val lineModifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
    if (isLarge) {
        Column(modifier = lineModifier, verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingLabel(label, Modifier.weight(1f))
                onChange?.let { onClick -> ChangeButton(rowLabel = label, onClick = onClick) }
            }
            value()
        }
    } else {
        Row(
            modifier = lineModifier.padding(top = RowVerticalPadding, bottom = if (isLast) 0.dp else RowVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        ) {
            SettingLabel(label, Modifier.width(LabelWidth))
            Box(modifier = Modifier.weight(1f)) { value() }
            onChange?.let { onClick -> ChangeButton(rowLabel = label, onClick = onClick) }
        }
    }
}

/**
 * 보이는 높이는 줄 이름과 같게 두고, 누르는 높이만 줄의 위아래 여백까지 넓혀 48로 맞춘다. 버튼 때문에 줄 높이가 바뀌지 않아야 한다.
 * 누르는 폭도 좌우로 넓히되 넓힌 만큼 겹쳐 놓아 값 태그가 쓸 폭을 줄이지 않는다.
 */
@Composable
private fun ChangeButton(
    rowLabel: String,
    onClick: () -> Unit,
) {
    val description = stringResource(Res.string.import_content_description_change_setting, rowLabel)

    Box(
        modifier =
            Modifier
                .overlapVertically(RowVerticalPadding)
                .overlapHorizontally(ChangeTouchSideExtension)
                .clickable(role = Role.Button, onClick = onClick)
                .clearAndSetSemantics { contentDescription = description }
                .padding(vertical = RowVerticalPadding, horizontal = ChangeTouchSideExtension)
                .heightIn(min = RowContentMinHeight)
                .padding(horizontal = CaroTheme.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.import_button_change),
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.brand,
        )
    }
}

@Composable
private fun SettingLabel(
    text: String,
    modifier: Modifier,
) {
    Box(modifier = modifier.heightIn(min = RowContentMinHeight), contentAlignment = Alignment.CenterStart) {
        Text(text = text, style = CaroTheme.typography.label1, color = CaroTheme.color.text.secondary)
    }
}

/** 공백뿐인 이름도 줄은 남겨야 [변경]으로 고칠 수 있다. 글자는 그리지 않아 스크린 리더가 빈 값을 읽지 않고 줄 이름만 읽는다. */
@Composable
private fun DeckNameValue(deckName: String) {
    Box(modifier = Modifier.heightIn(min = RowContentMinHeight), contentAlignment = Alignment.CenterStart) {
        if (deckName.isNotBlank()) {
            Text(
                text = deckName,
                style = CaroTheme.typography.body2.semiBold,
                color = CaroTheme.color.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingTags(tags: ImmutableList<SettingTag>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        maxLines = TAG_MAX_LINES,
    ) {
        tags.forEach { tag -> ImportInfoTag(text = tag.label()) }
    }
}

@Composable
private fun SettingTag.label(): String =
    when (this) {
        is SettingTag.SubdeckAll -> stringResource(Res.string.import_tag_subdeck_all, count.formatWithComma())
        is SettingTag.SubdeckSome -> stringResource(Res.string.import_tag_subdeck_some, total.formatWithComma(), selected.formatWithComma())
        SettingTag.SubdeckMerged -> stringResource(Res.string.import_tag_subdeck_merged)
        is SettingTag.NoteTypeCount -> stringResource(Res.string.import_tag_note_type_count, count.formatWithComma())
        SettingTag.SidesAuto -> stringResource(Res.string.import_tag_sides_auto)
        SettingTag.SidesManual -> stringResource(Res.string.import_tag_sides_manual)
        is SettingTag.SheetName -> name
        is SettingTag.HeaderRow -> stringResource(if (isOn) Res.string.import_tag_header_row_on else Res.string.import_tag_header_row_off)
        is SettingTag.Delimiter -> kind?.let { delimiter -> stringResource(delimiter.label()) } ?: text
        is SettingTag.FrontFields -> stringResource(Res.string.import_tag_front_fields, fieldList(names, moreCount))
        is SettingTag.BackFields -> stringResource(Res.string.import_tag_back_fields, fieldList(names, moreCount))
        SettingTag.BackNone -> stringResource(Res.string.import_tag_back_none)
        is SettingTag.EmptyCards -> stringResource(Res.string.import_tag_excluded_empty_cards, count.formatWithComma())
        is SettingTag.ImageOcclusionCards -> stringResource(Res.string.import_tag_excluded_image_occlusion, count.formatWithComma())
        is SettingTag.OtherSheets -> stringResource(Res.string.import_tag_excluded_other_sheets, count.formatWithComma())
    }

/** 이름 없는 열은 파서가 붙인 이름 대신 화면 언어의 열 번호로 보인다. */
@Composable
private fun fieldList(
    names: ImmutableList<TextCheckHeader>,
    moreCount: Int,
): String {
    val shown =
        names
            .map { name ->
                when (name) {
                    is TextCheckHeader.Named -> name.text
                    is TextCheckHeader.Numbered -> stringResource(Res.string.import_label_column, name.number)
                }
            }.joinToString(FIELD_SEPARATOR)
    return if (moreCount > 0) stringResource(Res.string.import_label_fields_more, shown, moreCount.formatWithComma()) else shown
}

/** 제외 줄은 바꿀 설정이 없다. */
private fun SettingRowKind.settingsKind(): SettingsKind? =
    when (this) {
        SettingRowKind.Subdeck -> SettingsKind.Subdeck
        SettingRowKind.Sheet -> SettingsKind.Sheet
        SettingRowKind.Delimiter -> SettingsKind.Delimiter
        SettingRowKind.CardLayout -> SettingsKind.CardLayout
        SettingRowKind.Excluded -> null
    }

private fun SettingRowKind.label(): StringResource =
    when (this) {
        SettingRowKind.Subdeck -> Res.string.import_label_setting_subdeck
        SettingRowKind.Sheet -> Res.string.import_label_setting_sheet
        SettingRowKind.Delimiter -> Res.string.import_label_delimiter
        SettingRowKind.CardLayout -> Res.string.import_label_setting_card_layout
        SettingRowKind.Excluded -> Res.string.import_label_setting_excluded
    }

internal fun DelimiterKind.label(): StringResource =
    when (this) {
        DelimiterKind.TAB -> Res.string.import_chip_delimiter_tab
        DelimiterKind.COMMA -> Res.string.import_chip_delimiter_comma
        DelimiterKind.SEMICOLON -> Res.string.import_chip_delimiter_semicolon
        DelimiterKind.PIPE -> Res.string.import_chip_delimiter_pipe
        DelimiterKind.SPACE -> Res.string.import_chip_delimiter_space
        DelimiterKind.COLON -> Res.string.import_chip_delimiter_colon
    }
