package com.whatever.caro.feature.learning.report.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import caromobile.core.designsystem.generated.resources.report_all_period_link
import caromobile.core.designsystem.generated.resources.report_card_deleted
import caromobile.core.designsystem.generated.resources.report_card_reviews_caption
import caromobile.core.designsystem.generated.resources.report_card_reviews_count
import caromobile.core.designsystem.generated.resources.report_card_reviews_current
import caromobile.core.designsystem.generated.resources.report_card_reviews_title
import caromobile.core.designsystem.generated.resources.report_error_description
import caromobile.core.designsystem.generated.resources.report_error_title
import caromobile.core.designsystem.generated.resources.report_history_date
import caromobile.core.designsystem.generated.resources.report_load_more_error
import caromobile.core.designsystem.generated.resources.report_retry
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.report.ReportCardLabel
import com.whatever.caro.core.model.report.ReportReview
import com.whatever.caro.feature.learning.report.card.mvi.CardReviewHistoryIntent
import com.whatever.caro.feature.learning.report.card.mvi.CardReviewHistoryState
import com.whatever.caro.feature.learning.report.components.ReportLoadMoreError
import com.whatever.caro.feature.learning.report.components.ReportMessage
import com.whatever.caro.feature.learning.report.components.ReportMinTouchSize
import com.whatever.caro.feature.learning.report.components.ReportPrimaryButton
import com.whatever.caro.feature.learning.report.components.ReportRatingLabel
import com.whatever.caro.feature.learning.report.components.ReportSkeleton
import com.whatever.caro.feature.learning.report.components.ReportTopBar
import com.whatever.caro.feature.learning.report.components.label
import com.whatever.caro.feature.learning.report.components.toRangeText
import com.whatever.caro.feature.learning.report.components.toReportTimeText
import org.jetbrains.compose.resources.stringResource

private const val LOAD_MORE_THRESHOLD = 3

@Composable
internal fun CardReviewHistoryScreen(
    state: CardReviewHistoryState,
    onIntent: (CardReviewHistoryIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(state.hasMore) {
        derivedStateOf {
            val lastVisible =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            state.hasMore && lastVisible >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) onIntent(CardReviewHistoryIntent.LoadMore) }

    Column(modifier = Modifier.fillMaxSize().background(color = CaroTheme.color.background.primary)) {
        ReportTopBar(title = stringResource(Res.string.report_card_reviews_title), onBack = { onIntent(CardReviewHistoryIntent.ClickBack) })
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
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xl2),
        ) {
            state.card?.let { card -> item(key = "card") { CurrentCard(card = card) } }
            item(key = "header") { HistoryHeader(state = state, onAllPeriod = { onIntent(CardReviewHistoryIntent.ClickAllPeriod) }) }
            item(key = "reviews") {
                Column {
                    state.reviews.forEach { review -> ReviewRow(review = review) }
                }
            }
            when {
                state.isLoading && state.reviews.isEmpty() -> {
                    item(key = "loading") { ReportSkeleton() }
                }

                state.hasError && state.reviews.isEmpty() -> {
                    item(key = "error") {
                        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l)) {
                            ReportMessage(
                                title = stringResource(Res.string.report_error_title),
                                description = stringResource(Res.string.report_error_description),
                            )
                            ReportPrimaryButton(
                                text = stringResource(Res.string.report_retry),
                                onClick = { onIntent(CardReviewHistoryIntent.Retry) },
                            )
                        }
                    }
                }

                state.hasError -> {
                    item(key = "more-error") {
                        ReportLoadMoreError(
                            message = stringResource(Res.string.report_load_more_error),
                            onRetry = { onIntent(CardReviewHistoryIntent.Retry) },
                        )
                    }
                }
            }
            if (state.hasLoaded) {
                item(key = "caption") {
                    Text(
                        text = stringResource(Res.string.report_card_reviews_caption),
                        style = CaroTheme.typography.caption1.regular,
                        color = CaroTheme.color.text.tertiary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentCard(card: ReportCardLabel) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = CaroTheme.color.surface.tertiary, shape = CaroTheme.shape.l)
                .padding(horizontal = CaroTheme.spacing.xl, vertical = CaroTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        Text(
            text = stringResource(Res.string.report_card_reviews_current),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
        if (card.deleted) {
            Text(
                text = stringResource(Res.string.report_card_deleted),
                style = CaroTheme.typography.heading1,
                color = CaroTheme.color.text.tertiary,
            )
        } else {
            Text(text = card.front.orEmpty(), style = CaroTheme.typography.heading1, color = CaroTheme.color.text.primary)
            Text(text = card.back.orEmpty(), style = CaroTheme.typography.body2.medium, color = CaroTheme.color.text.secondary)
        }
    }
}

@Composable
private fun HistoryHeader(
    state: CardReviewHistoryState,
    onAllPeriod: () -> Unit,
) {
    val range = state.period.toRangeText()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text =
                if (state.hasLoaded && state.hasMore.not()) {
                    stringResource(Res.string.report_card_reviews_count, range, state.reviews.size)
                } else {
                    range
                },
            modifier = Modifier.weight(1f),
            style = CaroTheme.typography.body1,
            color = CaroTheme.color.text.primary,
        )
        if (state.period != null) {
            Text(
                text = stringResource(Res.string.report_all_period_link),
                modifier =
                    Modifier
                        .heightIn(min = ReportMinTouchSize)
                        .noRippleClickable(role = Role.Button, onClick = onAllPeriod)
                        .padding(vertical = CaroTheme.spacing.m),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.secondary,
            )
        }
    }
}

@Composable
private fun ReviewRow(review: ReportReview) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = CaroTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs)) {
            Text(
                text =
                    stringResource(
                        Res.string.report_history_date,
                        review.studyDate.month.ordinal + 1,
                        review.studyDate.day,
                        stringResource(review.studyDate.dayOfWeek.label()),
                    ),
                style = CaroTheme.typography.body1,
                color = CaroTheme.color.text.primary,
            )
            Text(
                text = review.reviewedAt.time.toReportTimeText(),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.tertiary,
            )
        }
        ReportRatingLabel(rating = review.rating)
    }
}
