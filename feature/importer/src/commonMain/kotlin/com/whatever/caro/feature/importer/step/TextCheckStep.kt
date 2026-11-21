package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_hint_text_check_auto
import caromobile.core.designsystem.generated.resources.import_label_delimiter
import caromobile.core.designsystem.generated.resources.import_subtitle_text_check
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.feature.importer.components.ImportFieldChip
import com.whatever.caro.feature.importer.logic.buildTextCheckTable
import com.whatever.caro.feature.importer.logic.delimiterChips
import com.whatever.caro.feature.importer.logic.resolveDelimiter
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportSelection
import com.whatever.caro.feature.importer.transform.ImportCard
import org.jetbrains.compose.resources.stringResource

private const val HEADER_KEY = "text-check-header"
private const val DELIMITER_KEY = "delimiter"
private const val FIRST_CARD_KEY = "first-card"
private const val RESULT_KEY = "split-result"

/**
 * 다시 나누는 동안에도 칩과 체크를 눌러야 연달아 고른 값이 ViewModel에서 방금 고른 값에 이어진다.
 * 다시 나눈 표는 열 수가 달라지므로 표의 가로 위치만 처음으로 돌리고 세로 위치와 칩 노드는 남긴다.
 */
internal fun LazyListScope.textCheckItems(
    table: ParsedTable,
    selection: ImportSelection,
    firstCard: ImportCard?,
    isResplitting: Boolean,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    item(key = HEADER_KEY, contentType = HEADER_KEY) {
        SettingsHeader(
            fileName = null,
            title = stringResource(Res.string.import_subtitle_text_check),
            hint = stringResource(Res.string.import_hint_text_check_auto),
        )
    }
    item(key = DELIMITER_KEY, contentType = DELIMITER_KEY) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l),
        ) {
            DelimiterSetting(
                selected = resolveDelimiter(selection.delimiter, table.layout),
                onSelect = { delimiter -> onIntent(ImportDeckIntent.SelectDelimiter(delimiter)) },
            )
            HeaderRowSetting(
                checked = selection.hasHeaderRow ?: table.layout?.hasHeaderRow ?: false,
                columnsFromHeader = table.layout?.columnsFromHeader == true,
                onCheckedChange = { checked -> onIntent(ImportDeckIntent.ChangeHeaderRow(checked)) },
            )
        }
    }
    if (firstCard != null || isResplitting) {
        item(key = FIRST_CARD_KEY, contentType = FIRST_CARD_KEY) {
            FirstCardSection(
                card = firstCard,
                isLoading = isResplitting,
                modifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2).padding(top = CaroTheme.spacing.xl2),
            )
        }
    }
    item(key = RESULT_KEY, contentType = RESULT_KEY) {
        val checkTable = remember(table) { buildTextCheckTable(table) }
        val tableScrollState = rememberSaveable(table, saver = ScrollState.Saver) { ScrollState(initial = 0) }
        TextCheckResult(
            table = checkTable,
            isReparsing = isResplitting,
            tableScrollState = tableScrollState,
            modifier = Modifier.padding(top = CaroTheme.spacing.xl2),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DelimiterSetting(
    selected: Char,
    onSelect: (Char) -> Unit,
) {
    val chips = remember(selected) { delimiterChips(selected) }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
        Text(
            text = stringResource(Res.string.import_label_delimiter),
            style = CaroTheme.typography.body2.semiBold,
            color = CaroTheme.color.text.primary,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().chipRowTouchArea().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        ) {
            chips.forEach { chip ->
                ImportFieldChip(
                    text = chip.kind?.let { kind -> stringResource(kind.label()) } ?: chip.delimiter.toString(),
                    isSelected = chip.isSelected,
                    onClick = { onSelect(chip.delimiter) },
                    role = Role.RadioButton,
                )
            }
        }
    }
}
