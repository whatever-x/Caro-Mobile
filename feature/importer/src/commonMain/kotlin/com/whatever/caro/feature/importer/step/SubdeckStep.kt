package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.import_hint_subdeck_merge
import caromobile.core.designsystem.generated.resources.import_hint_subdeck_note_count
import caromobile.core.designsystem.generated.resources.import_label_card_count
import caromobile.core.designsystem.generated.resources.import_label_select_all
import caromobile.core.designsystem.generated.resources.import_subtitle_subdeck
import caromobile.core.designsystem.generated.resources.import_summary_limit
import caromobile.core.designsystem.generated.resources.import_summary_over
import caromobile.core.designsystem.generated.resources.import_summary_selected_label
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.model.importing.ImportLimits
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.components.ImportCheckbox
import com.whatever.caro.feature.importer.components.isLargeText
import com.whatever.caro.feature.importer.logic.selectedSubdeckCardCount
import com.whatever.caro.feature.importer.logic.subdeckCardCount
import com.whatever.caro.feature.importer.logic.subdeckDisplayNames
import com.whatever.caro.feature.importer.logic.subdeckSelectAllState
import com.whatever.caro.feature.importer.logic.truncatePath
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import org.jetbrains.compose.resources.stringResource

private const val HEADER_KEY = "subdeck-header"
private const val SELECT_ALL_KEY = "select-all"
private const val HINT_KEY = "subdeck-hint"
private const val SUBDECK_NAME_MAX_LINES = 3
private val SubdeckIndent = 32.dp

/**
 * 장수는 Image Occlusion을 뺀 카드 수라 상한 판정과 같은 기준이다. 전체 선택 줄은 하위 덱 합계이고 고른 합계는 하단 요약이 보인다.
 * 하위 덱 id를 그대로 키로 쓰면 다른 아이템의 키와 겹칠 수 있어 접두어를 붙인다.
 */
internal fun LazyListScope.subdeckItems(
    table: ParsedTable,
    selectedIds: Set<String>,
    gateNotice: String?,
    onIntent: (ImportDeckIntent) -> Unit,
) {
    val names = subdeckDisplayNames(table)
    val lastIndex = table.subdecks.size

    item(key = HEADER_KEY, contentType = HEADER_KEY) {
        SettingsHeader(
            fileName = table.sourceName,
            title = stringResource(Res.string.import_subtitle_subdeck),
            hint = stringResource(Res.string.import_hint_subdeck_merge),
            notice = gateNotice,
        )
    }
    item(key = SELECT_ALL_KEY, contentType = SELECT_ALL_KEY) {
        val selectAll = subdeckSelectAllState(table, selectedIds)
        SelectionCardRow(
            label = stringResource(Res.string.import_label_select_all),
            detail = cardCountText(table.subdecks.sumOf(::subdeckCardCount)),
            labelMaxLines = 1,
            position = CardPosition.of(0, lastIndex),
            interaction =
                Modifier.triStateToggleable(
                    state = selectAll,
                    role = Role.Checkbox,
                    onClick = { onIntent(ImportDeckIntent.ToggleAllSubdecks) },
                ),
            leading = { ImportCheckbox(state = selectAll) },
        )
    }
    itemsIndexed(table.subdecks, key = { _, subdeck -> "subdeck-${subdeck.id}" }) { index, subdeck ->
        val isChecked = subdeck.id in selectedIds
        SelectionCardRow(
            label = truncatePath(names[index]),
            detail = cardCountText(subdeckCardCount(subdeck)),
            labelMaxLines = SUBDECK_NAME_MAX_LINES,
            position = CardPosition.of(index + 1, lastIndex),
            interaction =
                Modifier.toggleable(
                    value = isChecked,
                    role = Role.Checkbox,
                    onValueChange = { onIntent(ImportDeckIntent.ToggleSubdeck(subdeck.id)) },
                ),
            leading = { ImportCheckbox(state = if (isChecked) ToggleableState.On else ToggleableState.Off) },
            indent = SubdeckIndent,
            showDivider = index == 0,
        )
    }
    item(key = HINT_KEY, contentType = HINT_KEY) {
        Text(
            text = stringResource(Res.string.import_hint_subdeck_note_count),
            modifier = Modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2).padding(top = CaroTheme.spacing.xl2),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
    }
}

/** 라벨, 합계, 상한을 한 live region으로 묶어 바뀔 때 한 번에 읽는다. 초과는 색만으로 알리지 않고 글자로도 표시한다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubdeckSummary(
    table: ParsedTable,
    selectedIds: Set<String>,
    isOverLimit: Boolean,
) {
    val total = selectedSubdeckCardCount(table, selectedIds).formatWithComma()
    val label = stringResource(Res.string.import_summary_selected_label)
    val amount =
        if (isOverLimit) {
            stringResource(
                Res.string.import_summary_over,
                total,
            )
        } else {
            stringResource(Res.string.import_label_card_count, total)
        }
    val limit = stringResource(Res.string.import_summary_limit, ImportLimits.MAX_CARDS.formatWithComma())
    val amountColor = if (isOverLimit) CaroTheme.color.text.error else CaroTheme.color.text.brand
    val summaryModifier =
        Modifier
            .fillMaxWidth()
            .padding(start = CaroTheme.spacing.xl2, top = CaroTheme.spacing.m, end = CaroTheme.spacing.xl2, bottom = CaroTheme.spacing.xs)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }

    if (isLargeText()) {
        Column(modifier = summaryModifier) {
            SummaryText(label)
            SummaryAmount(amount, amountColor)
            SummaryText(limit)
        }
    } else {
        FlowRow(
            modifier = summaryModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryText(label)
            Row(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                SummaryAmount(amount, amountColor)
                SummaryText(limit)
            }
        }
    }
}

@Composable
private fun cardCountText(count: Int): String = stringResource(Res.string.import_label_card_count, count.formatWithComma())

@Composable
private fun SummaryText(text: String) {
    Text(text = text, style = CaroTheme.typography.body2.medium, color = CaroTheme.color.text.secondary)
}

@Composable
private fun SummaryAmount(
    text: String,
    color: Color,
) {
    Text(text = text, style = CaroTheme.typography.body1, color = color)
}
