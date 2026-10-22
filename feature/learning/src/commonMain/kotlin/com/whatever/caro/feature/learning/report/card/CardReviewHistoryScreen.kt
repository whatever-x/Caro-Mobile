package com.whatever.caro.feature.learning.report.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_arrow_right_16
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
import caromobile.core.designsystem.generated.resources.report_sheet_recent7
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.report.ReportCardLabel
import com.whatever.caro.core.model.report.ReportReview
import com.whatever.caro.feature.learning.report.ReportPeriodPreset
import com.whatever.caro.feature.learning.report.ReportPeriods
import com.whatever.caro.feature.learning.report.SystemReportToday
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
import org.jetbrains.compose.resources.painterResource
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
    LaunchedEffect(shouldLoadMore, state.isLoading) {
        if (shouldLoadMore &&
            state.isLoading.not()
        ) {
            onIntent(CardReviewHistoryIntent.LoadMore)
        }
    }

    // 평가 행끼리는 붙이고 나머지 항목 사이만 띄운다.
    val itemGap = Modifier.padding(top = CaroTheme.spacing.xl2)

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
        ) {
            // 카드 정보가 늦게 와도 첫 항목 키가 바뀌지 않게 항상 둔다. 그렇지 않으면 목록이 헤더 위치를 유지해 카드가 위로 밀린다.
            item(key = "card") { state.card?.let { card -> CurrentCard(card = card) } }
            item(key = "header") {
                // 헤더는 터치 영역이 48dp라 Figma 간격보다 좁게 띄운다
                Box(modifier = if (state.card != null) Modifier.padding(top = CaroTheme.spacing.m) else Modifier) {
                    HistoryHeader(state = state, onAllPeriod = { onIntent(CardReviewHistoryIntent.ClickAllPeriod) })
                }
            }
            itemsIndexed(items = state.reviews, key = { _, review -> "review-${review.reviewId}" }) { index, review ->
                ReviewRow(review = review, modifier = if (index == 0) Modifier.padding(top = CaroTheme.spacing.s) else Modifier)
            }
            when {
                state.isLoading && state.reviews.isEmpty() -> {
                    item(key = "loading") { ReportSkeleton(modifier = itemGap) }
                }

                state.hasError && state.reviews.isEmpty() -> {
                    item(key = "error") {
                        Column(modifier = itemGap, verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l)) {
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
                            modifier = itemGap,
                        )
                    }
                }
            }
            if (state.hasLoaded) {
                item(key = "caption") {
                    Text(
                        text = stringResource(Res.string.report_card_reviews_caption),
                        modifier = itemGap,
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
    // 최근 7일 칩에서 들어온 기간은 칩 이름으로 보여준다
    val today = remember { SystemReportToday.today() }
    val range =
        if (state.period == ReportPeriods.presetPeriod(ReportPeriodPreset.RECENT_7, today)) {
            stringResource(Res.string.report_sheet_recent7)
        } else {
            state.period.toRangeText()
        }
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
            Row(
                modifier =
                    Modifier
                        .heightIn(min = ReportMinTouchSize)
                        .noRippleClickable(role = Role.Button, onClick = onAllPeriod)
                        .padding(vertical = CaroTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.report_all_period_link),
                    style = CaroTheme.typography.caption1.regular,
                    color = CaroTheme.color.text.secondary,
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_right_16),
                    contentDescription = null,
                    tint = CaroTheme.color.icon.secondary,
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(
    review: ReportReview,
    modifier: Modifier = Modifier,
) {
    val dividerColor = CaroTheme.color.border.secondary
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }.padding(vertical = CaroTheme.spacing.l),
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
