package com.whatever.caro.feature.importer.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import caromobile.core.designsystem.generated.resources.import_button_next
import caromobile.core.designsystem.generated.resources.import_hint_subdeck_empty_notes
import caromobile.core.designsystem.generated.resources.import_hint_subdeck_merge
import caromobile.core.designsystem.generated.resources.import_hint_subdeck_no_deck_notes
import caromobile.core.designsystem.generated.resources.import_hint_subdeck_note_count
import caromobile.core.designsystem.generated.resources.import_label_card_count
import caromobile.core.designsystem.generated.resources.import_label_item_count
import caromobile.core.designsystem.generated.resources.import_label_no_deck
import caromobile.core.designsystem.generated.resources.import_label_select_all
import caromobile.core.designsystem.generated.resources.import_reason_subdeck_over_limit
import caromobile.core.designsystem.generated.resources.import_subtitle_subdeck
import caromobile.core.designsystem.generated.resources.import_summary_limit
import caromobile.core.designsystem.generated.resources.import_summary_over
import caromobile.core.designsystem.generated.resources.import_summary_selected_label
import com.whatever.caro.core.designsystem.components.CaroCheckbox
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.importparser.model.ParsedSubdeck
import com.whatever.caro.core.importparser.model.ParsedTable
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import com.whatever.caro.feature.importer.logic.commonParentPath
import com.whatever.caro.feature.importer.logic.noDeckCount
import com.whatever.caro.feature.importer.logic.selectAllState
import com.whatever.caro.feature.importer.logic.subdeckDisplayName
import com.whatever.caro.feature.importer.logic.summarizeSubdeckSelection
import com.whatever.caro.feature.importer.logic.truncatePath
import kotlinx.collections.immutable.PersistentSet
import org.jetbrains.compose.resources.stringResource

private const val HEADER_KEY = "header"
private const val HINTS_KEY = "hints"
private const val SUBDECK_NAME_MAX_LINES = 3
private val SubdeckIndent = 32.dp

/** 하위 덱 id를 그대로 키로 쓰면 다른 줄의 키와 겹칠 수 있어 접두어를 붙인다. */
private sealed interface SubdeckListRow {
    val key: String

    data class SelectAll(
        val noteCount: Int,
    ) : SubdeckListRow {
        override val key: String = "select-all"
    }

    data class Subdeck(
        val id: String,
        val name: String,
        val noteCount: Int,
    ) : SubdeckListRow {
        override val key: String = "subdeck-$id"
    }

    data class NoDeck(
        val noteCount: Int,
    ) : SubdeckListRow {
        override val key: String = "subdeck-${ParsedSubdeck.NO_DECK_ID}"
    }
}

/** 장수는 노트 수다. 전체 선택 줄은 하위 덱 합계이고 선택한 합계는 하단 요약이 보여준다. */
@Composable
internal fun SubdeckStep(
    table: ParsedTable,
    selectedIds: PersistentSet<String>,
    maxCards: Int,
    onToggleSubdeck: (String) -> Unit,
    onToggleAll: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(table) { subdeckRows(table) }
    val selection = remember(table, selectedIds, maxCards) { summarizeSubdeckSelection(table, selectedIds, maxCards) }
    val selectAll = remember(table, selectedIds) { selectAllState(table, selectedIds) }
    val title = stringResource(Res.string.import_subtitle_subdeck)
    val hint = stringResource(Res.string.import_hint_subdeck_merge)
    val reason = if (selection.isOverLimit) stringResource(Res.string.import_reason_subdeck_over_limit) else null

    SelectionStepFrame(
        buttonText = stringResource(Res.string.import_button_next),
        onNext = onNext,
        enabled = selection.selectedTotal > 0 && !selection.isOverLimit,
        reason = reason,
        modifier = modifier,
        summary = {
            SelectionSummary(
                selectedTotal = selection.selectedTotal,
                isOverLimit = selection.isOverLimit,
                maxCards = maxCards,
            )
        },
    ) {
        item(key = HEADER_KEY) {
            SelectionHeader(fileName = table.sourceName, title = title, hint = hint)
        }
        itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
            SubdeckListItem(
                row = row,
                position = CardPosition.of(index, rows.lastIndex),
                showDivider = index > 0 && (row is SubdeckListRow.NoDeck || rows[index - 1] is SubdeckListRow.SelectAll),
                selectAll = selectAll,
                isSelected = { id -> id in selectedIds },
                onToggleSubdeck = onToggleSubdeck,
                onToggleAll = onToggleAll,
            )
        }
        item(key = HINTS_KEY) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2, vertical = CaroTheme.spacing.xl2),
            ) {
                SubdeckHint(stringResource(Res.string.import_hint_subdeck_note_count))
                SubdeckHint(stringResource(Res.string.import_hint_subdeck_empty_notes))
            }
        }
    }
}

private fun subdeckRows(table: ParsedTable): List<SubdeckListRow> {
    val commonParent = commonParentPath(table.subdecks.map { subdeck -> subdeck.path })
    val noDeck = noDeckCount(table)
    return buildList {
        add(SubdeckListRow.SelectAll(noteCount = table.subdecks.sumOf { subdeck -> subdeck.noteCount }))
        table.subdecks.forEach { subdeck ->
            add(
                SubdeckListRow.Subdeck(
                    id = subdeck.id,
                    name = truncatePath(subdeckDisplayName(subdeck.path, commonParent)),
                    noteCount = subdeck.noteCount,
                ),
            )
        }
        if (noDeck > 0) add(SubdeckListRow.NoDeck(noteCount = noDeck))
    }
}

@Composable
private fun SubdeckListItem(
    row: SubdeckListRow,
    position: CardPosition,
    showDivider: Boolean,
    selectAll: ToggleableState,
    isSelected: (String) -> Boolean,
    onToggleSubdeck: (String) -> Unit,
    onToggleAll: () -> Unit,
) {
    when (row) {
        is SubdeckListRow.SelectAll -> {
            SelectionCardRow(
                label = stringResource(Res.string.import_label_select_all),
                detail = stringResource(Res.string.import_label_card_count, row.noteCount.formatWithComma()),
                labelMaxLines = 1,
                position = position,
                interaction = Modifier.triStateToggleable(state = selectAll, role = Role.Checkbox, onClick = onToggleAll),
                leading = { CaroCheckbox(state = selectAll) },
            )
        }

        is SubdeckListRow.Subdeck -> {
            val checked = isSelected(row.id)
            SelectionCardRow(
                label = row.name,
                detail = stringResource(Res.string.import_label_card_count, row.noteCount.formatWithComma()),
                labelMaxLines = SUBDECK_NAME_MAX_LINES,
                position = position,
                interaction = Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggleSubdeck(row.id) }),
                leading = { CaroCheckbox(state = checked.toToggleableState()) },
                indent = SubdeckIndent,
                showDivider = showDivider,
            )
        }

        is SubdeckListRow.NoDeck -> {
            val checked = isSelected(ParsedSubdeck.NO_DECK_ID)
            SelectionCardRow(
                label = stringResource(Res.string.import_label_no_deck),
                detail = stringResource(Res.string.import_label_item_count, row.noteCount.formatWithComma()),
                labelMaxLines = 1,
                position = position,
                interaction =
                    Modifier.toggleable(
                        value = checked,
                        role = Role.Checkbox,
                        onValueChange = { onToggleSubdeck(ParsedSubdeck.NO_DECK_ID) },
                    ),
                leading = { CaroCheckbox(state = checked.toToggleableState()) },
                caption = stringResource(Res.string.import_hint_subdeck_no_deck_notes),
                showDivider = showDivider,
            )
        }
    }
}

private fun Boolean.toToggleableState(): ToggleableState = if (this) ToggleableState.On else ToggleableState.Off

@Composable
private fun SubdeckHint(text: String) {
    Text(
        text = text,
        style = CaroTheme.typography.caption1.regular,
        color = CaroTheme.color.text.secondary,
    )
}

/** 라벨, 합계, 상한을 한 live region으로 묶어 바뀔 때 한 번에 읽는다. 초과는 색만으로 알리지 않고 글자로도 표시한다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionSummary(
    selectedTotal: Int,
    isOverLimit: Boolean,
    maxCards: Int,
) {
    val label = stringResource(Res.string.import_summary_selected_label)
    val amount =
        if (isOverLimit) {
            stringResource(Res.string.import_summary_over, selectedTotal.formatWithComma())
        } else {
            stringResource(Res.string.import_label_card_count, selectedTotal.formatWithComma())
        }
    val limit = stringResource(Res.string.import_summary_limit, maxCards.formatWithComma())
    val amountColor = if (isOverLimit) CaroTheme.color.text.error else CaroTheme.color.text.brand
    val summaryModifier =
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }

    if (isLargeText()) {
        Column(modifier = summaryModifier) {
            SummaryLabel(label)
            SummaryAmount(amount, amountColor)
            SummaryLimit(limit)
        }
    } else {
        FlowRow(
            modifier = summaryModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryLabel(label)
            Row(
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SummaryAmount(amount, amountColor)
                SummaryLimit(limit)
            }
        }
    }
}

@Composable
private fun SummaryLabel(text: String) {
    Text(text = text, style = CaroTheme.typography.label1, color = CaroTheme.color.text.secondary)
}

@Composable
private fun SummaryAmount(
    text: String,
    color: Color,
) {
    Text(text = text, style = CaroTheme.typography.body1, color = color)
}

@Composable
private fun SummaryLimit(text: String) {
    Text(text = text, style = CaroTheme.typography.label1, color = CaroTheme.color.text.primary)
}
