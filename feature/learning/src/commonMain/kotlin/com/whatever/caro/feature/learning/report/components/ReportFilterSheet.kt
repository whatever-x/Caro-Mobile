package com.whatever.caro.feature.learning.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_left_24
import caromobile.core.designsystem.generated.resources.ic_arrow_right_24
import caromobile.core.designsystem.generated.resources.ic_check_24
import caromobile.core.designsystem.generated.resources.ic_x_24
import caromobile.core.designsystem.generated.resources.report_date_back
import caromobile.core.designsystem.generated.resources.report_date_done
import caromobile.core.designsystem.generated.resources.report_date_end
import caromobile.core.designsystem.generated.resources.report_date_guide
import caromobile.core.designsystem.generated.resources.report_date_month
import caromobile.core.designsystem.generated.resources.report_date_next_month
import caromobile.core.designsystem.generated.resources.report_date_not_selected
import caromobile.core.designsystem.generated.resources.report_date_prev_month
import caromobile.core.designsystem.generated.resources.report_date_start
import caromobile.core.designsystem.generated.resources.report_date_title
import caromobile.core.designsystem.generated.resources.report_scope_all
import caromobile.core.designsystem.generated.resources.report_scope_deleted_deck
import caromobile.core.designsystem.generated.resources.report_sheet_apply
import caromobile.core.designsystem.generated.resources.report_sheet_cancel
import caromobile.core.designsystem.generated.resources.report_sheet_custom
import caromobile.core.designsystem.generated.resources.report_sheet_deck
import caromobile.core.designsystem.generated.resources.report_sheet_decks_error
import caromobile.core.designsystem.generated.resources.report_sheet_period
import caromobile.core.designsystem.generated.resources.report_sheet_recent30
import caromobile.core.designsystem.generated.resources.report_sheet_recent7
import caromobile.core.designsystem.generated.resources.report_sheet_title
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.report.ReportDeckOption
import com.whatever.caro.feature.learning.report.ReportPeriodPreset
import com.whatever.caro.feature.learning.report.mvi.ReportDateEditorState
import com.whatever.caro.feature.learning.report.mvi.ReportSheetState
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CalendarDaySize = 40.dp
private const val DAYS_PER_WEEK = 7
private const val LOAD_MORE_THRESHOLD = 3

/**
 * 조회 조건 시트. 상단, 기간, "전체"는 고정하고 덱 목록만 스크롤한다.
 * X, 바깥 터치, 뒤로 가기는 고른 값을 버린다. 기간 직접 설정이 열려 있으면 뒤로 가기는 그 화면만 닫는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportFilterSheet(
    sheet: ReportSheetState,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onSelectPreset: (ReportPeriodPreset) -> Unit,
    onSelectAllDecks: () -> Unit,
    onSelectDeck: (ReportDeckOption) -> Unit,
    onLoadMoreDecks: () -> Unit,
    onCloseDateEditor: () -> Unit,
    onClickDate: (LocalDate) -> Unit,
    onMoveMonth: (Int) -> Unit,
    onConfirmDateRange: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { if (sheet.dateEditor != null) onCloseDateEditor() else onDismiss() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = CaroTheme.spacing.xl2, topEnd = CaroTheme.spacing.xl2),
        containerColor = CaroTheme.color.surface.primary,
        dragHandle = { SheetHandle() },
    ) {
        val editor = sheet.dateEditor
        if (editor != null) {
            ReportDateEditor(
                editor = editor,
                onBack = onCloseDateEditor,
                onClickDate = onClickDate,
                onMoveMonth = onMoveMonth,
                onConfirm = onConfirmDateRange,
            )
        } else {
            ReportFilterContent(
                sheet = sheet,
                onCancel = onDismiss,
                onApply = onApply,
                onSelectPreset = onSelectPreset,
                onSelectAllDecks = onSelectAllDecks,
                onSelectDeck = onSelectDeck,
                onLoadMoreDecks = onLoadMoreDecks,
            )
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = CaroTheme.spacing.s), contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(color = CaroTheme.color.divider.primary, shape = CaroTheme.shape.xxs),
        )
    }
}

@Composable
private fun ReportFilterContent(
    sheet: ReportSheetState,
    onCancel: () -> Unit,
    onApply: () -> Unit,
    onSelectPreset: (ReportPeriodPreset) -> Unit,
    onSelectAllDecks: () -> Unit,
    onSelectDeck: (ReportDeckOption) -> Unit,
    onLoadMoreDecks: () -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(sheet) {
        derivedStateOf {
            val lastVisible =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            sheet.hasMoreDecks && lastVisible >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onLoadMoreDecks() }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2).padding(bottom = CaroTheme.spacing.xl3),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xl),
    ) {
        SheetHeader(
            title = stringResource(Res.string.report_sheet_title),
            leading = {
                SheetIconButton(
                    icon = Res.drawable.ic_x_24,
                    description = stringResource(Res.string.report_sheet_cancel),
                    onClick = onCancel,
                )
            },
            trailing = {
                SheetIconButton(
                    icon = Res.drawable.ic_check_24,
                    description = stringResource(Res.string.report_sheet_apply),
                    onClick = onApply,
                    enabled = sheet.canApply,
                )
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
            SheetSubtitle(stringResource(Res.string.report_sheet_period))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
                verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
            ) {
                listOf(
                    ReportPeriodPreset.RECENT_7 to Res.string.report_sheet_recent7,
                    ReportPeriodPreset.RECENT_30 to Res.string.report_sheet_recent30,
                    ReportPeriodPreset.CUSTOM to Res.string.report_sheet_custom,
                ).forEach { (preset, label) ->
                    ReportChip(
                        text = stringResource(label),
                        isSelected = sheet.draft.preset == preset,
                        onClick = { onSelectPreset(preset) },
                    )
                }
            }
            Text(
                text = sheet.draft.period.toRangeText(),
                style = CaroTheme.typography.body2.medium,
                color = CaroTheme.color.text.secondary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
            SheetSubtitle(stringResource(Res.string.report_sheet_deck))
            DeckOptionRow(
                name = stringResource(Res.string.report_scope_all),
                isSelected = sheet.draft.deckId == null,
                onClick = onSelectAllDecks,
            )
            Spacer(modifier = Modifier.fillMaxWidth().height(1.dp).background(CaroTheme.color.divider.secondary))
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                items(items = sheet.decks, key = { it.deckId }) { deck ->
                    DeckOptionRow(
                        name = deck.name ?: stringResource(Res.string.report_scope_deleted_deck),
                        isSelected = sheet.draft.deckId == deck.deckId,
                        onClick = { onSelectDeck(deck) },
                    )
                }
                if (sheet.hasDecksError) {
                    item {
                        ReportLoadMoreError(message = stringResource(Res.string.report_sheet_decks_error), onRetry = onLoadMoreDecks)
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    leading: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        leading()
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = CaroTheme.typography.heading1,
            color = CaroTheme.color.text.primary,
            textAlign = TextAlign.Center,
        )
        trailing()
    }
}

@Composable
private fun SheetIconButton(
    icon: org.jetbrains.compose.resources.DrawableResource,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier.size(ReportMinTouchSize).noRippleClickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = if (enabled) CaroTheme.color.icon.brand else CaroTheme.color.icon.disabled,
        )
    }
}

@Composable
private fun SheetSubtitle(text: String) {
    Text(text = text, style = CaroTheme.typography.body2.semiBold, color = CaroTheme.color.text.secondary)
}

@Composable
internal fun ReportChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .heightIn(min = ReportMinTouchSize)
                .noRippleClickable(role = Role.Tab, onClick = onClick)
                .semantics { selected = isSelected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier =
                Modifier
                    .background(
                        color = if (isSelected) CaroTheme.color.surface.tertiary else CaroTheme.color.surface.primary,
                        shape = CaroTheme.shape.xxl,
                    ).border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) CaroTheme.color.border.brand else CaroTheme.color.border.secondary,
                        shape = CaroTheme.shape.xxl,
                    ).padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.s),
            style = if (isSelected) CaroTheme.typography.body2.semiBold else CaroTheme.typography.body2.medium,
            color = if (isSelected) CaroTheme.color.text.primary else CaroTheme.color.text.secondary,
        )
    }
}

@Composable
private fun DeckOptionRow(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(
                    color = if (isSelected) CaroTheme.color.surface.tertiary else CaroTheme.color.surface.primary,
                    shape = CaroTheme.shape.m,
                ).noRippleClickable(role = Role.RadioButton, onClick = onClick)
                .semantics { selected = isSelected }
                .padding(CaroTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = if (isSelected) CaroTheme.typography.body1 else CaroTheme.typography.heading3,
            color = CaroTheme.color.text.primary,
        )
        Box(modifier = Modifier.size(24.dp)) {
            if (isSelected) {
                Icon(
                    painter = painterResource(Res.drawable.ic_check_24),
                    contentDescription = null,
                    tint = CaroTheme.color.icon.brand,
                )
            }
        }
    }
}

/**
 * 기간 직접 설정. 처음 누른 날짜가 시작일, 다음 날짜가 종료일이다. "기간 선택 완료"는 조회 조건 시트로 돌아가기만 한다.
 */
@Composable
private fun ReportDateEditor(
    editor: ReportDateEditorState,
    onBack: () -> Unit,
    onClickDate: (LocalDate) -> Unit,
    onMoveMonth: (Int) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = CaroTheme.spacing.xl2).padding(bottom = CaroTheme.spacing.xl3),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xl),
    ) {
        SheetHeader(
            title = stringResource(Res.string.report_date_title),
            leading = {
                SheetIconButton(
                    icon = Res.drawable.ic_arrow_left_24,
                    description = stringResource(Res.string.report_date_back),
                    onClick = onBack,
                )
            },
            trailing = { Spacer(modifier = Modifier.size(ReportMinTouchSize)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
            DateField(
                label = stringResource(Res.string.report_date_start),
                date = editor.startDate,
                isActive = editor.startDate == null || editor.endDate != null,
                modifier = Modifier.weight(1f),
            )
            DateField(
                label = stringResource(Res.string.report_date_end),
                date = editor.endDate,
                isActive = editor.startDate != null && editor.endDate == null,
                modifier = Modifier.weight(1f),
            )
        }
        Calendar(editor = editor, onClickDate = onClickDate, onMoveMonth = onMoveMonth)
        Text(
            text = stringResource(Res.string.report_date_guide),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
        ReportPrimaryButton(
            text = stringResource(Res.string.report_date_done),
            onClick = onConfirm,
            enabled = editor.canConfirm,
        )
    }
}

@Composable
private fun DateField(
    label: String,
    date: LocalDate?,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(color = CaroTheme.color.surface.primary, shape = CaroTheme.shape.m)
                .border(
                    width = if (isActive) 1.5.dp else 1.dp,
                    color = if (isActive) CaroTheme.color.border.brand else CaroTheme.color.border.secondary,
                    shape = CaroTheme.shape.m,
                ).padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
    ) {
        Text(text = label, style = CaroTheme.typography.caption1.regular, color = CaroTheme.color.text.secondary)
        Text(
            text = date?.toReportDateText() ?: stringResource(Res.string.report_date_not_selected),
            style = CaroTheme.typography.body1,
            color = if (date == null) CaroTheme.color.text.tertiary else CaroTheme.color.text.primary,
        )
    }
}

@Composable
private fun Calendar(
    editor: ReportDateEditorState,
    onClickDate: (LocalDate) -> Unit,
    onMoveMonth: (Int) -> Unit,
) {
    val month = editor.visibleMonth
    val isCurrentMonth = month.year == editor.today.year && month.month == editor.today.month
    Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SheetIconButton(
                icon = Res.drawable.ic_arrow_left_24,
                description = stringResource(Res.string.report_date_prev_month),
                onClick = { onMoveMonth(-1) },
            )
            Text(
                text = stringResource(Res.string.report_date_month, month.year, month.month.ordinal + 1),
                modifier = Modifier.weight(1f),
                style = CaroTheme.typography.body1,
                color = CaroTheme.color.text.primary,
                textAlign = TextAlign.Center,
            )
            SheetIconButton(
                icon = Res.drawable.ic_arrow_right_24,
                description = stringResource(Res.string.report_date_next_month),
                onClick = { onMoveMonth(1) },
                enabled = isCurrentMonth.not(),
            )
        }
        val weekStart = listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weekStart.forEach { day ->
                Text(
                    text = stringResource(day.label()),
                    modifier = Modifier.width(CalendarDaySize),
                    style = CaroTheme.typography.caption1.regular,
                    color = CaroTheme.color.text.tertiary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // 일요일 시작 달력에서 1일 앞의 빈칸 수
        val leadingBlanks = month.dayOfWeek.isoDayNumber % DAYS_PER_WEEK
        val daysInMonth = generateSequence(month) { it.plus(DatePeriod(days = 1)) }.takeWhile { it.month == month.month }.toList()
        val cells = List(leadingBlanks) { null } + daysInMonth
        cells.chunked(DAYS_PER_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (week + List(DAYS_PER_WEEK - week.size) { null }).forEach { date ->
                    CalendarDay(date = date, editor = editor, onClick = onClickDate)
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate?,
    editor: ReportDateEditorState,
    onClick: (LocalDate) -> Unit,
) {
    if (date == null) {
        Spacer(modifier = Modifier.size(CalendarDaySize))
        return
    }
    val start = editor.startDate
    val end = editor.endDate
    val isEdge = date == start || date == end
    val isInRange = start != null && end != null && date > start && date < end
    val isDisabled = date > editor.today
    Box(
        modifier =
            Modifier
                .size(CalendarDaySize)
                .background(
                    color =
                        when {
                            isEdge -> CaroTheme.color.surface.brand
                            isInRange -> CaroTheme.color.surface.tertiary
                            else -> CaroTheme.color.surface.primary
                        },
                    shape = CaroTheme.shape.xxl,
                ).noRippleClickable(enabled = isDisabled.not(), role = Role.Button) { onClick(date) }
                .semantics { selected = isEdge || isInRange },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.day.toString(),
            style = if (isEdge) CaroTheme.typography.body2.semiBold else CaroTheme.typography.body2.medium,
            color =
                when {
                    isEdge -> CaroTheme.color.text.inverse
                    isDisabled -> CaroTheme.color.text.disabled
                    else -> CaroTheme.color.text.primary
                },
        )
    }
}
