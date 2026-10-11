package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_button_next
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_colon
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_comma
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_pipe
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_semicolon
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_space
import caromobile.core.designsystem.generated.resources.import_chip_delimiter_tab
import caromobile.core.designsystem.generated.resources.import_hint_text_check_auto
import caromobile.core.designsystem.generated.resources.import_label_delimiter
import caromobile.core.designsystem.generated.resources.import_label_header_from_file
import caromobile.core.designsystem.generated.resources.import_subtitle_text_check
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.component.ImportBottomBar
import com.whatever.caro.feature.importer.component.ImportStepTitle
import com.whatever.caro.feature.importer.logic.DelimiterChip
import com.whatever.caro.feature.importer.logic.DelimiterKind
import com.whatever.caro.feature.importer.logic.buildTextCheckTable
import com.whatever.caro.feature.importer.logic.delimiterChips
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource

private val ChipBorderWidth = 1.dp
private val ChipHorizontalPadding = 12.dp
private val ChipVerticalPadding = 6.dp

/**
 * [isReparsing] 동안에도 칩과 체크를 눌러야 연달아 고른 값이 ViewModel에서 방금 고른 값에 이어진다.
 * 세로 스크롤 컨테이너는 이 화면에 하나만 두고 가로 스크롤은 결과 표에만 둔다.
 */
@Composable
internal fun TextCheckStep(
    table: ParsedTable,
    delimiter: Char,
    hasHeaderRow: Boolean,
    isReparsing: Boolean,
    onSelectDelimiter: (Char) -> Unit,
    onChangeHeaderRow: (Boolean) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val checkTable = remember(table) { buildTextCheckTable(table) }
    val chips = remember(delimiter) { delimiterChips(delimiter).toImmutableList() }
    val verticalScrollState = rememberScrollState()
    val tableScrollState = rememberScrollState()

    // 다시 나눈 결과는 열 수가 달라지므로 표의 가로 위치만 처음으로 돌리고 세로 위치와 포커스는 남긴다.
    val firstTable = remember { table }
    LaunchedEffect(table) { if (table !== firstTable) tableScrollState.scrollTo(0) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(verticalScrollState)
                    .padding(top = CaroTheme.spacing.xl, bottom = CaroTheme.spacing.xl2),
        ) {
            TextCheckGuide()
            DelimiterSetting(chips = chips, onSelect = onSelectDelimiter)
            if (table.layout?.columnsFromHeader == true) {
                Text(
                    text = stringResource(Res.string.import_label_header_from_file),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = CaroTheme.spacing.xl2)
                            .padding(top = CaroTheme.spacing.l, bottom = CaroTheme.spacing.xl2),
                    style = CaroTheme.typography.label1,
                    color = CaroTheme.color.text.secondary,
                )
            } else {
                HeaderRowToggle(
                    checked = hasHeaderRow,
                    onCheckedChange = onChangeHeaderRow,
                    modifier = Modifier.padding(top = CaroTheme.spacing.xs, bottom = CaroTheme.spacing.m),
                )
            }
            TextCheckResult(table = checkTable, isReparsing = isReparsing, tableScrollState = tableScrollState)
        }
        StepBottomDivider(isVisible = verticalScrollState.canScrollForward || verticalScrollState.canScrollBackward)
        ImportBottomBar(
            buttonText = stringResource(Res.string.import_button_next),
            onClick = onNext,
            enabled = !isReparsing,
        )
    }
}

@Composable
private fun TextCheckGuide() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        ImportStepTitle(text = stringResource(Res.string.import_subtitle_text_check))
        Text(
            text = stringResource(Res.string.import_hint_text_check_auto),
            style = CaroTheme.typography.label1,
            color = CaroTheme.color.text.secondary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DelimiterSetting(
    chips: ImmutableList<DelimiterChip>,
    onSelect: (Char) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = CaroTheme.spacing.xl2)
                .padding(top = CaroTheme.spacing.xl2),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        Text(
            text = stringResource(Res.string.import_label_delimiter),
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        ) {
            chips.forEach { chip -> DelimiterChipButton(chip = chip, onSelect = onSelect) }
        }
    }
}

/** 선택해도 테두리 두께가 같아 칩 높이와 글자 기준선이 흔들리지 않는다. */
@Composable
private fun DelimiterChipButton(
    chip: DelimiterChip,
    onSelect: (Char) -> Unit,
) {
    val shape = CaroTheme.shape.xxl
    val fillColor = if (chip.isSelected) CaroTheme.color.surface.brand else CaroTheme.color.surface.primary
    val borderColor = if (chip.isSelected) CaroTheme.color.border.brand else CaroTheme.color.border.secondary
    Text(
        text = chip.label(),
        modifier =
            Modifier
                .clip(shape)
                .background(fillColor)
                .border(ChipBorderWidth, borderColor, shape)
                .selectable(selected = chip.isSelected, role = Role.RadioButton, onClick = { onSelect(chip.delimiter) })
                .padding(horizontal = ChipHorizontalPadding, vertical = ChipVerticalPadding),
        style = CaroTheme.typography.label1,
        color = if (chip.isSelected) CaroTheme.color.text.inverse else CaroTheme.color.text.secondary,
    )
}

@Composable
private fun DelimiterChip.label(): String =
    when (kind) {
        DelimiterKind.TAB -> stringResource(Res.string.import_chip_delimiter_tab)
        DelimiterKind.COMMA -> stringResource(Res.string.import_chip_delimiter_comma)
        DelimiterKind.SEMICOLON -> stringResource(Res.string.import_chip_delimiter_semicolon)
        DelimiterKind.PIPE -> stringResource(Res.string.import_chip_delimiter_pipe)
        DelimiterKind.SPACE -> stringResource(Res.string.import_chip_delimiter_space)
        DelimiterKind.COLON -> stringResource(Res.string.import_chip_delimiter_colon)
        null -> delimiter.toString()
    }
