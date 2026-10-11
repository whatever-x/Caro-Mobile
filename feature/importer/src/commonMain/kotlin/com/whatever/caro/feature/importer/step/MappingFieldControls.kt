package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_a11y_field_selected
import caromobile.core.designsystem.generated.resources.import_a11y_field_unavailable
import caromobile.core.designsystem.generated.resources.import_a11y_pick_fields
import caromobile.core.designsystem.generated.resources.import_button_pick_fields
import caromobile.core.designsystem.generated.resources.import_label_fields_more
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedNoteType
import com.whatever.caro.core.model.importing.FieldMapping
import com.whatever.caro.feature.importer.component.FieldPickerSheet
import com.whatever.caro.feature.importer.logic.pickerRows
import com.whatever.caro.feature.importer.logic.summarizeChosenFields
import com.whatever.caro.feature.importer.logic.truncateChip
import com.whatever.caro.feature.importer.logic.usesFieldPicker
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

private val ChipBorderWidth = 1.dp
private val ChipHorizontalPadding = 12.dp
private val ChipVerticalPadding = 6.dp
private val ChipContentGap = 6.dp
private val OrderBadgeMinSize = 18.dp
private val PickerButtonMinHeight = 48.dp
private const val CHIP_NAME_MAX_LINES = 2
private const val SUMMARY_MAX_LINES = 2

/** 한 면의 라벨과 이름이다. [heading]은 면 제목과 시트 제목이고 [name]은 접근성 문장에서 면을 부르는 이름이다. */
internal data class FieldSideSpec(
    val target: String,
    val heading: String,
    val name: String,
    val otherName: String,
)

@Composable
internal fun FieldSide(
    type: ParsedNoteType,
    mapping: FieldMapping,
    side: FieldSideSpec,
    isEnabled: Boolean,
    onToggleField: (noteTypeId: String, target: String, fieldName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chosen = mapping.targets[side.target].orEmpty()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
        Text(
            text = side.heading,
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
        if (usesFieldPicker(type)) {
            FieldPickerControl(
                type = type,
                mapping = mapping,
                side = side,
                isEnabled = isEnabled,
                onToggleField = onToggleField,
            )
        } else {
            FieldChips(type = type, chosen = chosen, side = side, isEnabled = isEnabled, onToggleField = onToggleField)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FieldChips(
    type: ParsedNoteType,
    chosen: List<String>,
    side: FieldSideSpec,
    isEnabled: Boolean,
    onToggleField: (noteTypeId: String, target: String, fieldName: String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        type.fieldNames.forEach { fieldName ->
            FieldChip(
                name = fieldName,
                order = chosen.indexOf(fieldName).takeIf { index -> index >= 0 }?.let { index -> index + 1 },
                sideName = side.name,
                isEnabled = isEnabled,
                onClick = { onToggleField(type.id, side.target, fieldName) },
            )
        }
    }
}

/**
 * 선택해도 테두리 두께가 같아 칩 높이와 글자 기준선이 흔들리지 않는다. 높이는 고정하지 않아 큰 글자에서 커진다.
 * 고른 칩은 안쪽 글자와 번호를 한 문장으로 읽도록 하위 semantics를 지우고, 누르는 동작은 바깥 toggleable이 맡는다.
 */
@Composable
private fun FieldChip(
    name: String,
    order: Int?,
    sideName: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = CaroTheme.shape.xxl
    val isChosen = order != null
    val label = truncateChip(name)
    val chosenDescription = order?.let { number -> stringResource(Res.string.import_a11y_field_selected, label, sideName, number) }
    val unavailable = stringResource(Res.string.import_a11y_field_unavailable)

    Row(
        modifier =
            Modifier
                .clip(shape)
                .background(if (isChosen) CaroTheme.color.surface.brand else CaroTheme.color.surface.primary)
                .border(ChipBorderWidth, if (isChosen) CaroTheme.color.border.brand else CaroTheme.color.border.secondary, shape)
                .toggleable(value = isChosen, enabled = isEnabled, role = Role.Checkbox, onValueChange = { onClick() })
                .then(
                    when {
                        chosenDescription != null -> {
                            Modifier.clearAndSetSemantics {
                                contentDescription = chosenDescription
                                if (!isEnabled) stateDescription = unavailable
                            }
                        }

                        !isEnabled -> {
                            Modifier.semantics { stateDescription = unavailable }
                        }

                        else -> {
                            Modifier
                        }
                    },
                ).padding(horizontal = ChipHorizontalPadding, vertical = ChipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(ChipContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (order != null) OrderBadge(order)
        Text(
            text = label,
            style = CaroTheme.typography.label1,
            color = if (isChosen) CaroTheme.color.text.inverse else CaroTheme.color.text.secondary,
            maxLines = CHIP_NAME_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 최소 크기만 두어 글자 크기를 따라 커진다. */
@Composable
private fun OrderBadge(order: Int) {
    Box(
        modifier =
            Modifier
                .defaultMinSize(minWidth = OrderBadgeMinSize, minHeight = OrderBadgeMinSize)
                .background(CaroTheme.color.surface.primary, CaroTheme.shape.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = order.toString(),
            style = CaroTheme.typography.caption1.medium,
            color = CaroTheme.color.text.brand,
        )
    }
}

/**
 * 항목이 많은 유형의 한 면이다. 요약과 시트를 여는 버튼을 보여주고 시트를 닫으면 버튼으로 포커스를 돌린다.
 * 시트가 열린 채 상태를 복원해도 이어서 보이도록 열림 여부는 rememberSaveable로 둔다.
 */
@Composable
private fun FieldPickerControl(
    type: ParsedNoteType,
    mapping: FieldMapping,
    side: FieldSideSpec,
    isEnabled: Boolean,
    onToggleField: (noteTypeId: String, target: String, fieldName: String) -> Unit,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    var returnsFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val chosen = mapping.targets[side.target].orEmpty()
    val summary = chosenSummaryText(chosen)
    val button: @Composable () -> Unit = {
        PickerButton(
            text = stringResource(Res.string.import_button_pick_fields, type.fieldNames.size, chosen.size),
            description = stringResource(Res.string.import_a11y_pick_fields, side.name, type.fieldNames.size, chosen.size),
            isEnabled = isEnabled,
            onClick = { isOpen = true },
            modifier = Modifier.focusRequester(focusRequester),
        )
    }

    // 큰 글자에서는 요약과 버튼이 한 줄에 들어가지 않으므로 위아래로 쌓는다.
    if (isLargeText()) {
        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs)) {
            if (summary.isNotEmpty()) SummaryText(summary)
            button()
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (summary.isNotEmpty()) SummaryText(summary, modifier = Modifier.weight(1f)) else Spacer(modifier = Modifier.weight(1f))
            button()
        }
    }

    if (isOpen) {
        FieldPickerSheet(
            title = side.heading,
            sideLabel = side.name,
            otherSideLabel = side.otherName,
            rows = remember(type, mapping, side.target) { pickerRows(type.fieldNames, mapping, side.target).toImmutableList() },
            onToggleField = { fieldName -> onToggleField(type.id, side.target, fieldName) },
            onDismiss = {
                isOpen = false
                returnsFocus = true
            },
        )
    }

    // 닫기 요청을 처리하는 시점에는 시트 창이 아직 붙어 있어, 창을 걷은 다음 구성에서 포커스를 요청한다.
    LaunchedEffect(returnsFocus) {
        if (returnsFocus) {
            focusRequester.requestFocus()
            returnsFocus = false
        }
    }
}

/** 고른 항목이 없으면 빈 문자열이다. */
@Composable
private fun chosenSummaryText(chosen: List<String>): String {
    val summary = summarizeChosenFields(chosen)
    val names = summary.names.joinToString(", ") { name -> truncateChip(name) }
    return if (summary.moreCount > 0) stringResource(Res.string.import_label_fields_more, names, summary.moreCount) else names
}

@Composable
private fun SummaryText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = text,
        style = CaroTheme.typography.label1,
        color = CaroTheme.color.text.primary,
        maxLines = SUMMARY_MAX_LINES,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 눌리는 높이는 [PickerButtonMinHeight] 이상이고 보이는 모양은 그 안의 테두리 알약이다. 접근성 문장은 면을 넣은 [description]이다.
 * clickable만으로는 터치 모드에서 포커스 요청을 받지 않으므로 focusable을 따로 둔다.
 */
@Composable
private fun PickerButton(
    text: String,
    description: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CaroTheme.shape.xxl
    val unavailable = stringResource(Res.string.import_a11y_field_unavailable)

    Box(
        modifier =
            modifier
                .heightIn(min = PickerButtonMinHeight)
                .focusable(enabled = isEnabled)
                .clickable(enabled = isEnabled, role = Role.Button, onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = description
                    if (!isEnabled) stateDescription = unavailable
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier =
                Modifier
                    .clip(shape)
                    .border(ChipBorderWidth, CaroTheme.color.border.secondary, shape)
                    .padding(horizontal = CaroTheme.spacing.m, vertical = ChipVerticalPadding),
            text = text,
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.secondary,
        )
    }
}
