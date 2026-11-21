package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_hint_sheet_only_selected
import caromobile.core.designsystem.generated.resources.import_label_row_count
import caromobile.core.designsystem.generated.resources.import_label_row_over_limit
import caromobile.core.designsystem.generated.resources.import_label_sheet_empty
import caromobile.core.designsystem.generated.resources.import_subtitle_sheet
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedSheet
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.logic.SheetRowLabel
import com.whatever.caro.feature.importer.logic.sheetRowLabel
import com.whatever.caro.feature.importer.logic.truncateSheetName
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportSelection
import org.jetbrains.compose.resources.stringResource

private const val HEADER_KEY = "sheet-header"
private const val SHEETS_KEY = "sheets"
private const val HEADER_ROW_KEY = "header-row"
private const val SHEET_NAME_MAX_LINES = 2
private val RadioSize = 22.dp
private val RadioDotSize = 8.dp
private val RadioBorderWidth = 1.dp

/**
 * 빈 시트와 상한을 넘는 시트도 고를 수 있고, 고르면 사유와 함께 버튼이 꺼진다.
 * 라디오가 한 묶음으로 읽히려면 selectableGroup 하나가 모든 시트 줄을 감싸야 하므로 시트 줄을 아이템 하나에 그린다.
 */
internal fun LazyListScope.sheetItems(
    table: ParsedTable,
    selection: ImportSelection,
    gateNotice: String?,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    item(key = HEADER_KEY, contentType = HEADER_KEY) {
        SettingsHeader(
            fileName = table.sourceName,
            title = stringResource(Res.string.import_subtitle_sheet),
            hint = stringResource(Res.string.import_hint_sheet_only_selected),
            notice = gateNotice,
        )
    }
    item(key = SHEETS_KEY, contentType = SHEETS_KEY) {
        Column(modifier = Modifier.selectableGroup()) {
            table.sheets.forEachIndexed { index, sheet ->
                val isSelected = sheet.index == selection.sheetIndex
                SelectionCardRow(
                    label = truncateSheetName(sheet.name),
                    detail = sheetRowText(sheet),
                    labelMaxLines = SHEET_NAME_MAX_LINES,
                    position = CardPosition.of(index, table.sheets.lastIndex),
                    interaction =
                        Modifier.selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onIntent(ImportDeckIntent.SelectSheet(sheet.index)) },
                        ),
                    leading = { RadioMark(isSelected = isSelected) },
                )
            }
        }
    }
    item(key = HEADER_ROW_KEY, contentType = HEADER_ROW_KEY) {
        HeaderRowSetting(
            checked = selection.hasHeaderRow ?: table.layout?.hasHeaderRow ?: false,
            columnsFromHeader = table.layout?.columnsFromHeader == true,
            onCheckedChange = { checked -> onIntent(ImportDeckIntent.ChangeHeaderRow(checked)) },
            modifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2).padding(top = CaroTheme.spacing.xl2),
        )
    }
}

@Composable
private fun sheetRowText(sheet: ParsedSheet): String =
    when (val label = sheetRowLabel(sheet, ImportLimits.MAX_CARDS)) {
        SheetRowLabel.Empty -> stringResource(Res.string.import_label_sheet_empty)
        is SheetRowLabel.Rows -> stringResource(Res.string.import_label_row_count, label.count.formatWithComma())
        SheetRowLabel.OverLimit -> stringResource(Res.string.import_label_row_over_limit, ImportLimits.MAX_CARDS.formatWithComma())
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
            Box(modifier = Modifier.size(RadioDotSize).clip(CaroTheme.shape.xxl).background(CaroTheme.color.icon.inverse))
        }
    }
}
