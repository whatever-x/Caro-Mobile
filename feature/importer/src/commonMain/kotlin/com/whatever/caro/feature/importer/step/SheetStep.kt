package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_button_next
import caromobile.core.designsystem.generated.resources.import_hint_sheet_only_selected
import caromobile.core.designsystem.generated.resources.import_label_row_count
import caromobile.core.designsystem.generated.resources.import_label_row_over_limit
import caromobile.core.designsystem.generated.resources.import_label_sheet_empty
import caromobile.core.designsystem.generated.resources.import_reason_sheet_empty
import caromobile.core.designsystem.generated.resources.import_subtitle_sheet
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.logic.SheetRowLabel
import com.whatever.caro.feature.importer.logic.isSheetEmpty
import com.whatever.caro.feature.importer.logic.sheetRowLabel
import com.whatever.caro.feature.importer.logic.truncateSheetName
import org.jetbrains.compose.resources.stringResource

private const val HEADER_KEY = "header"
private const val HEADER_ROW_KEY = "header-row"
private const val SHEET_NAME_MAX_LINES = 2
private val RadioSize = 22.dp
private val RadioDotSize = 8.dp
private val RadioBorderWidth = 1.dp

/** 빈 시트도 고를 수 있지만 [다음]은 막는다. 행 수가 상한을 넘는 시트는 막지 않고 미리보기 단계에서 막는다. */
@Composable
internal fun SheetStep(
    table: ParsedTable,
    sheetIndex: Int,
    hasHeaderRow: Boolean,
    maxCards: Int,
    onSelectSheet: (Int) -> Unit,
    onChangeHeaderRow: (Boolean) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelectedSheetEmpty = isSheetEmpty(table, sheetIndex)
    val title = stringResource(Res.string.import_subtitle_sheet)
    val hint = stringResource(Res.string.import_hint_sheet_only_selected)
    val reason = if (isSelectedSheetEmpty) stringResource(Res.string.import_reason_sheet_empty) else null

    SelectionStepFrame(
        buttonText = stringResource(Res.string.import_button_next),
        onNext = onNext,
        enabled = !isSelectedSheetEmpty,
        reason = reason,
        modifier = modifier,
    ) {
        item(key = HEADER_KEY) {
            SelectionHeader(fileName = table.sourceName, title = title, hint = hint)
        }
        itemsIndexed(table.sheets, key = { _, sheet -> "sheet-${sheet.index}" }) { index, sheet ->
            val isSelected = sheet.index == sheetIndex
            SelectionCardRow(
                label = truncateSheetName(sheet.name),
                detail = sheetRowText(sheet, maxCards),
                labelMaxLines = SHEET_NAME_MAX_LINES,
                position = CardPosition.of(index, table.sheets.lastIndex),
                interaction = Modifier.selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelectSheet(sheet.index) }),
                leading = { RadioMark(isSelected = isSelected) },
            )
        }
        item(key = HEADER_ROW_KEY) {
            HeaderRowToggle(
                checked = hasHeaderRow,
                onCheckedChange = onChangeHeaderRow,
                modifier = Modifier.padding(top = CaroTheme.spacing.m, bottom = CaroTheme.spacing.xl2),
            )
        }
    }
}

@Composable
private fun sheetRowText(
    sheet: ParsedSheet,
    maxCards: Int,
): String =
    when (val label = sheetRowLabel(sheet, maxCards)) {
        SheetRowLabel.Empty -> stringResource(Res.string.import_label_sheet_empty)
        is SheetRowLabel.Rows -> stringResource(Res.string.import_label_row_count, label.count.formatWithComma())
        SheetRowLabel.OverLimit -> stringResource(Res.string.import_label_row_over_limit, maxCards.formatWithComma())
    }

/** 상태 semantics는 부모 줄의 selectable이 맡으므로 이 표시는 장식이다. */
@Composable
private fun RadioMark(isSelected: Boolean) {
    Box(
        modifier =
            Modifier
                .size(RadioSize)
                .clip(CaroTheme.shape.xxl)
                .background(if (isSelected) CaroTheme.color.surface.brand else Color.Transparent)
                .border(
                    width = RadioBorderWidth,
                    color = if (isSelected) CaroTheme.color.border.brand else CaroTheme.color.border.secondary,
                    shape = CaroTheme.shape.xxl,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier =
                    Modifier
                        .size(RadioDotSize)
                        .clip(CaroTheme.shape.xxl)
                        .background(CaroTheme.color.icon.inverse),
            )
        }
    }
}
