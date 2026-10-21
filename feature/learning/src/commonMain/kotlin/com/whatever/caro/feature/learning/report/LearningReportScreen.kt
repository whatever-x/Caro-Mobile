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
import caromobile.core.designsystem.generated.resources.report_lifetime_title
import caromobile.core.designsystem.generated.resources.report_load_more_error
import caromobile.core.designsystem.generated.resources.report_retry
import caromobile.core.designsystem.generated.resources.report_selected_title
import caromobile.core.designsystem.generated.resources.report_title
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.learning.StudyRating
import com.whatever.caro.core.model.report.ReportCardRecord
import com.whatever.caro.core.model.report.StudyReportSummary
import com.whatever.caro.feature.learning.report.components.ReportChart
import com.whatever.caro.feature.learning.report.components.ReportChip
import com.whatever.caro.feature.learning.report.components.ReportFilterSheet
import com.whatever.caro.feature.learning.report.components.ReportLifetimeCard
import com.whatever.caro.feature.learning.report.components.ReportLinkRow
import com.whatever.caro.feature.learning.report.components.ReportLoadMoreError
import com.whatever.caro.feature.learning.report.components.ReportMessage
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
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xl3),
        ) {
            item(key = "scope") {
                ReportScopeHeader(
                    deckId = state.filter.deckId,
                    deckName = state.filter.deckName,
                    deckDeleted = state.filter.deckDeleted,
                    period = state.filter.period,
                    isSheetOpen = state.sheet != null,
                    onClick = { onIntent(LearningReportIntent.OpenSheet) },
                )
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

    state.sheet?.let { sheet ->
        ReportFilterSheet(
            sheet = sheet,
            onDismiss = { onIntent(LearningReportIntent.DismissSheet) },
            onApply = { onIntent(LearningReportIntent.ApplySheet) },
            onSelectPreset = { onIntent(LearningReportIntent.SelectDraftPreset(it)) },
            onSelectAllDecks = { onIntent(LearningReportIntent.SelectDraftAllDecks) },
            onSelectDeck = { onIntent(LearningReportIntent.SelectDraftDeck(it)) },
            onLoadMoreDecks = { onIntent(LearningReportIntent.LoadMoreDecks) },
            onCloseDateEditor = { onIntent(LearningReportIntent.CloseDateEditor) },
            onClickDate = { onIntent(LearningReportIntent.ClickDate(it)) },
            onMoveMonth = { onIntent(LearningReportIntent.MoveMonth(it)) },
            onConfirmDateRange = { onIntent(LearningReportIntent.ConfirmDateRange) },
        )
    }
}

private fun LazyListScope.summaryItems(
    state: LearningReportState,
    summary: StudyReportSummary,
    onIntent: (LearningReportIntent) -> Unit,
) {
    val lifetime = summary.lifetime
    // "전체"인데 누적 기록도 없으면 시작 안내만 보여준다
    if (state.isDeckReport.not() && lifetime != null && lifetime.reviewCount == 0L) {
        item(key = "empty-all") {
            ReportMessage(
                title = stringResource(Res.string.report_empty_all_title),
                description = stringResource(Res.string.report_empty_all_description),
            )
        }
        return
    }
    if (lifetime != null) {
        item(key = "lifetime") { ReportLifetimeCard(title = stringResource(Res.string.report_lifetime_title), counts = lifetime) }
    }
    item(key = "selected") {
        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
            ReportSectionTitle(title = stringResource(Res.string.report_selected_title))
            ReportStats(counts = summary.selected)
        }
    }
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
    item(key = "chart") {
        ReportChart(buckets = summary.buckets, unit = summary.range.bucketUnit, restDates = summary.restDates)
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
