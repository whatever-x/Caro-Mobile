package com.whatever.caro.feature.learning.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_right_16
import caromobile.core.designsystem.generated.resources.report_all_period_link
import caromobile.core.designsystem.generated.resources.report_card_deleted
import caromobile.core.designsystem.generated.resources.report_card_meta
import caromobile.core.designsystem.generated.resources.report_cards_caption
import caromobile.core.designsystem.generated.resources.report_cards_title
import caromobile.core.designsystem.generated.resources.report_empty_all_description
import caromobile.core.designsystem.generated.resources.report_empty_all_title
import caromobile.core.designsystem.generated.resources.report_empty_period_description
import caromobile.core.designsystem.generated.resources.report_empty_period_title
import caromobile.core.designsystem.generated.resources.report_error_description
import caromobile.core.designsystem.generated.resources.report_error_title
import caromobile.core.designsystem.generated.resources.report_filter_all
import caromobile.core.designsystem.generated.resources.report_history_link
import caromobile.core.designsystem.generated.resources.report_load_more_error
import caromobile.core.designsystem.generated.resources.report_retry
import caromobile.core.designsystem.generated.resources.report_title
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportCardRecord
import com.whatever.caro.core.model.report.ReportPeriod
import com.whatever.caro.core.model.report.StudyReportSummary
import com.whatever.caro.feature.learning.report.components.ReportChart
import com.whatever.caro.feature.learning.report.components.ReportChip
import com.whatever.caro.feature.learning.report.components.ReportDateSheet
import com.whatever.caro.feature.learning.report.components.ReportDeckSheet
import com.whatever.caro.feature.learning.report.components.ReportLinkRow
import com.whatever.caro.feature.learning.report.components.ReportLoadMoreError
import com.whatever.caro.feature.learning.report.components.ReportMessage
import com.whatever.caro.feature.learning.report.components.ReportMonthDetailPanel
import com.whatever.caro.feature.learning.report.components.ReportPeriodChips
import com.whatever.caro.feature.learning.report.components.ReportPrimaryButton
import com.whatever.caro.feature.learning.report.components.ReportRatingDistribution
import com.whatever.caro.feature.learning.report.components.ReportRatingLabel
import com.whatever.caro.feature.learning.report.components.ReportScopeHeader
import com.whatever.caro.feature.learning.report.components.ReportSectionTitle
import com.whatever.caro.feature.learning.report.components.ReportSkeleton
import com.whatever.caro.feature.learning.report.components.ReportStats
import com.whatever.caro.feature.learning.report.components.ReportTopBar
import com.whatever.caro.feature.learning.report.components.formatCount
import com.whatever.caro.feature.learning.report.components.label
import com.whatever.caro.feature.learning.report.components.toShortDateText
import com.whatever.caro.feature.learning.report.mvi.LearningReportIntent
import com.whatever.caro.feature.learning.report.mvi.LearningReportState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val LOAD_MORE_THRESHOLD = 3
private val SectionSpacing = 28.dp

@Composable
internal fun LearningReportScreen(
    state: LearningReportState,
    onIntent: (LearningReportIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMoreCards by remember(state.hasMoreCards, state.isDeckReport) {
        derivedStateOf {
            val lastVisible =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            state.isDeckReport && state.hasMoreCards && lastVisible >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMoreCards) { if (shouldLoadMoreCards) onIntent(LearningReportIntent.LoadMoreCards) }

    Column(modifier = Modifier.fillMaxSize().background(color = CaroTheme.color.background.primary)) {
        ReportTopBar(title = stringResource(Res.string.report_title), onBack = { onIntent(LearningReportIntent.ClickBack) })
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = CaroTheme.spacing.xl2,
                    end = CaroTheme.spacing.xl2,
                    top = CaroTheme.spacing.s,
                    bottom = CaroTheme.spacing.xl4,
                ),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing),
        ) {
            item(key = "scope") {
                val range = state.summary?.range
                Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l)) {
                    ReportScopeHeader(
                        deckId = state.filter.deckId,
                        deckName = state.filter.deckName,
                        deckDeleted = state.filter.deckDeleted,
                        // "전체"는 서버가 정한 첫 학습일부터 오늘까지를 보여준다
                        period = state.filter.period ?: range?.startDate?.let { ReportPeriod(it, range.endDate) },
                        isSheetOpen = state.deckSheet != null,
                        onClick = { onIntent(LearningReportIntent.OpenDeckSheet) },
                    )
                    ReportPeriodChips(
                        selected = state.filter.preset,
                        onSelect = { onIntent(LearningReportIntent.SelectPreset(it)) },
                    )
                }
            }
            val summary = state.summary
            when {
                state.isSummaryLoading -> {
                    item(key = "loading") { ReportSkeleton() }
                }

                state.hasSummaryError || summary == null -> {
                    item(
                        key = "error",
                    ) { ReportError(onRetry = { onIntent(LearningReportIntent.ClickRetry) }) }
                }

                else -> {
                    summaryItems(state = state, summary = summary, onIntent = onIntent)
                }
            }
        }
    }

    state.deckSheet?.let { sheet ->
        ReportDeckSheet(
            sheet = sheet,
            selectedDeckId = state.filter.deckId,
            onDismiss = { onIntent(LearningReportIntent.DismissDeckSheet) },
            onSelectAllDecks = { onIntent(LearningReportIntent.SelectAllDecks) },
            onSelectDeck = { onIntent(LearningReportIntent.SelectDeck(it)) },
            onLoadMoreDecks = { onIntent(LearningReportIntent.LoadMoreDecks) },
        )
    }
    state.dateEditor?.let { editor ->
        ReportDateSheet(
            editor = editor,
            onDismiss = { onIntent(LearningReportIntent.DismissDateEditor) },
            onClickDate = { onIntent(LearningReportIntent.ClickDate(it)) },
            onMoveMonth = { onIntent(LearningReportIntent.MoveMonth(it)) },
            onSelectQuickRange = { onIntent(LearningReportIntent.SelectQuickRange(it)) },
            onConfirm = { onIntent(LearningReportIntent.ConfirmDateRange) },
        )
    }
}

private fun LazyListScope.summaryItems(
    state: LearningReportState,
    summary: StudyReportSummary,
    onIntent: (LearningReportIntent) -> Unit,
) {
    // 전체 덱의 전체 기간에도 기록이 없으면 시작 안내만 보여준다
    if (state.isDeckReport.not() && state.filter.period == null && summary.selected.reviewCount == 0L) {
        item(key = "empty-all") {
            ReportMessage(
                title = stringResource(Res.string.report_empty_all_title),
                description = stringResource(Res.string.report_empty_all_description),
            )
        }
        return
    }
    item(key = "selected") { ReportStats(counts = summary.selected) }
    if (summary.selected.reviewCount == 0L) {
        item(key = "empty-period") {
            Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l)) {
                ReportMessage(
                    title = stringResource(Res.string.report_empty_period_title),
                    description = stringResource(Res.string.report_empty_period_description),
                )
                if (state.filter.period != null) {
                    ReportLinkRow(
                        text = stringResource(Res.string.report_all_period_link),
                        onClick = { onIntent(LearningReportIntent.ClickAllPeriod) },
                    )
                }
            }
        }
        return
    }
    // 하루만 골랐거나 학습일이 하루뿐이면 막대 하나라 차트를 숨긴다
    if (summary.range.startDate != summary.range.endDate && summary.selected.studyDayCount > 1) {
        item(key = "chart") {
            Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l)) {
                ReportChart(
                    buckets = summary.buckets,
                    unit = summary.range.bucketUnit,
                    selected = state.monthDetail?.bucket,
                    onClickBar = { onIntent(LearningReportIntent.ClickBar(it)) },
                )
                state.monthDetail?.let { detail ->
                    ReportMonthDetailPanel(
                        detail = detail,
                        onClickMonthAll = { onIntent(LearningReportIntent.ClickMonthAll) },
                        onClickWeek = { onIntent(LearningReportIntent.ClickWeek(it)) },
                        onRetry = { onIntent(LearningReportIntent.RetryMonthDetail) },
                    )
                }
            }
        }
    }
    item(key = "ratings") { ReportRatingDistribution(counts = summary.selected) }
    item(key = "history") {
        ReportLinkRow(text = stringResource(Res.string.report_history_link), onClick = { onIntent(LearningReportIntent.ClickHistory) })
    }
    if (state.isDeckReport) cardItems(state = state, onIntent = onIntent)
}

private fun LazyListScope.cardItems(
    state: LearningReportState,
    onIntent: (LearningReportIntent) -> Unit,
) {
    item(key = "cards-title") {
        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
            ReportSectionTitle(
                title = stringResource(Res.string.report_cards_title),
                caption = stringResource(Res.string.report_cards_caption),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s)) {
                ReportChip(
                    text = stringResource(Res.string.report_filter_all),
                    isSelected = state.cardRating == null,
                    onClick = { onIntent(LearningReportIntent.SelectCardRating(null)) },
                )
                listOf(StudyRating.EASY, StudyRating.FAIR, StudyRating.AGAIN).forEach { rating ->
                    ReportChip(
                        text = stringResource(rating.label()),
                        isSelected = state.cardRating == rating,
                        onClick = { onIntent(LearningReportIntent.SelectCardRating(rating)) },
                    )
                }
            }
        }
    }
    items(items = state.cards, key = { "card-${it.card.cardId}" }) { record ->
        ReportCardRow(record = record, onClick = { onIntent(LearningReportIntent.ClickCard(record.card.cardId)) })
    }
    when {
        state.hasCardsError && state.cards.isEmpty() -> {
            item(key = "cards-error") { ReportError(onRetry = { onIntent(LearningReportIntent.RetryCards) }) }
        }

        state.hasCardsError -> {
            item(key = "cards-more-error") {
                ReportLoadMoreError(
                    message = stringResource(Res.string.report_load_more_error),
                    onRetry = { onIntent(LearningReportIntent.RetryCards) },
                )
            }
        }
    }
}

@Composable
private fun ReportCardRow(
    record: ReportCardRecord,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .noRippleClickable(role = Role.Button, onClick = onClick)
                .padding(vertical = CaroTheme.spacing.l),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs)) {
            if (record.card.deleted) {
                Text(
                    text = stringResource(Res.string.report_card_deleted),
                    style = CaroTheme.typography.body1,
                    color = CaroTheme.color.text.tertiary,
                )
            } else {
                Text(text = record.card.front.orEmpty(), style = CaroTheme.typography.body1, color = CaroTheme.color.text.primary)
                Text(text = record.card.back.orEmpty(), style = CaroTheme.typography.body2.medium, color = CaroTheme.color.text.secondary)
            }
            Text(
                text =
                    stringResource(
                        Res.string.report_card_meta,
                        record.reviewCount.formatCount(),
                        record.lastStudyDate.toShortDateText(),
                    ),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.tertiary,
            )
        }
        ReportRatingLabel(rating = record.lastRating)
        Icon(
            painter = painterResource(Res.drawable.ic_arrow_right_16),
            contentDescription = null,
            tint = CaroTheme.color.icon.secondary,
        )
    }
}

@Composable
private fun ReportError(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l)) {
        ReportMessage(
            title = stringResource(Res.string.report_error_title),
            description = stringResource(Res.string.report_error_description),
        )
        ReportPrimaryButton(text = stringResource(Res.string.report_retry), onClick = onRetry)
    }
}
