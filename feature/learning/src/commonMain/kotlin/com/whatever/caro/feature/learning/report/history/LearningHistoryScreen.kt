package com.whatever.caro.feature.learning.report.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.learning_again
import caromobile.core.designsystem.generated.resources.learning_easy
import caromobile.core.designsystem.generated.resources.learning_fair
import caromobile.core.designsystem.generated.resources.report_all_period_link
import caromobile.core.designsystem.generated.resources.report_empty_period_description
import caromobile.core.designsystem.generated.resources.report_empty_period_title
import caromobile.core.designsystem.generated.resources.report_error_description
import caromobile.core.designsystem.generated.resources.report_error_title
import caromobile.core.designsystem.generated.resources.report_history_caption
import caromobile.core.designsystem.generated.resources.report_history_count
import caromobile.core.designsystem.generated.resources.report_history_date
import caromobile.core.designsystem.generated.resources.report_history_distribution
import caromobile.core.designsystem.generated.resources.report_history_title
import caromobile.core.designsystem.generated.resources.report_load_more_error
import caromobile.core.designsystem.generated.resources.report_retry
import caromobile.core.designsystem.generated.resources.report_scope_all
import caromobile.core.designsystem.generated.resources.report_scope_deleted_deck
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.report.ReportSession
import com.whatever.caro.feature.learning.report.components.ReportLinkRow
import com.whatever.caro.feature.learning.report.components.ReportLoadMoreError
import com.whatever.caro.feature.learning.report.components.ReportMessage
import com.whatever.caro.feature.learning.report.components.ReportPrimaryButton
import com.whatever.caro.feature.learning.report.components.ReportSkeleton
import com.whatever.caro.feature.learning.report.components.ReportTopBar
import com.whatever.caro.feature.learning.report.components.formatCount
import com.whatever.caro.feature.learning.report.components.label
import com.whatever.caro.feature.learning.report.components.toRangeText
import com.whatever.caro.feature.learning.report.history.mvi.LearningHistoryIntent
import com.whatever.caro.feature.learning.report.history.mvi.LearningHistoryState
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private const val LOAD_MORE_THRESHOLD = 3

@Composable
internal fun LearningHistoryScreen(
    state: LearningHistoryState,
    onIntent: (LearningHistoryIntent) -> Unit,
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
            onIntent(LearningHistoryIntent.LoadMore)
        }
    }
    val groups = remember(state.sessions) { state.sessions.groupBy { it.studyDate }.toList() }

    Column(modifier = Modifier.fillMaxSize().background(color = CaroTheme.color.background.primary)) {
        ReportTopBar(title = stringResource(Res.string.report_history_title), onBack = { onIntent(LearningHistoryIntent.ClickBack) })
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
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xl),
        ) {
            item(key = "scope") {
                Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs)) {
                    Text(
                        text =
                            when {
                                state.deckId == null -> stringResource(Res.string.report_scope_all)
                                state.deckName == null -> stringResource(Res.string.report_scope_deleted_deck)
                                else -> state.deckName
                            },
                        modifier = Modifier.padding(vertical = CaroTheme.spacing.m),
                        style = CaroTheme.typography.heading1,
                        color = CaroTheme.color.text.primary,
                    )
                    Text(
                        text = state.period.toRangeText(),
                        style = CaroTheme.typography.body2.medium,
                        color = CaroTheme.color.text.secondary,
                    )
                }
            }
            groups.forEach { (date, sessions) ->
                item(key = "date-$date") {
                    Text(text = date.toHistoryDateText(), style = CaroTheme.typography.body1, color = CaroTheme.color.text.primary)
                }
                items(items = sessions, key = { "session-${it.sessionId}" }) { session -> SessionRow(session = session) }
            }
            when {
                state.isLoading && state.sessions.isEmpty() -> {
                    item(key = "loading") { ReportSkeleton() }
                }

                state.hasError && state.sessions.isEmpty() -> {
                    item(key = "error") {
                        Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.l)) {
                            ReportMessage(
                                title = stringResource(Res.string.report_error_title),
                                description = stringResource(Res.string.report_error_description),
                            )
                            ReportPrimaryButton(
                                text = stringResource(Res.string.report_retry),
                                onClick = { onIntent(LearningHistoryIntent.Retry) },
                            )
                        }
                    }
                }

                state.hasError -> {
                    item(key = "more-error") {
                        ReportLoadMoreError(
                            message = stringResource(Res.string.report_load_more_error),
                            onRetry = { onIntent(LearningHistoryIntent.Retry) },
                        )
                    }
                }

                state.hasLoaded && state.sessions.isEmpty() -> {
                    item(key = "empty") {
                        ReportMessage(
                            title = stringResource(Res.string.report_empty_period_title),
                            description = stringResource(Res.string.report_empty_period_description),
                        )
                    }
                }
            }
            if (state.hasLoaded && state.hasMore.not()) {
                item(key = "footer") {
                    Column(verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
                        if (state.period != null) {
                            ReportLinkRow(
                                text = stringResource(Res.string.report_all_period_link),
                                onClick = { onIntent(LearningHistoryIntent.ClickAllPeriod) },
                            )
                        }
                        Text(
                            text = stringResource(Res.string.report_history_caption),
                            style = CaroTheme.typography.caption1.regular,
                            color = CaroTheme.color.text.tertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: ReportSession) {
    val ratings = session.counts.ratings
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = CaroTheme.color.surface.primary, shape = CaroTheme.shape.m)
                .border(width = 1.dp, color = CaroTheme.color.border.secondary, shape = CaroTheme.shape.m)
                .padding(horizontal = CaroTheme.spacing.xl, vertical = CaroTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m)) {
            Text(
                text = if (session.deckDeleted) stringResource(Res.string.report_scope_deleted_deck) else session.deckName.orEmpty(),
                modifier = Modifier.weight(1f),
                style = CaroTheme.typography.body1,
                color = if (session.deckDeleted) CaroTheme.color.text.tertiary else CaroTheme.color.text.primary,
            )
            Text(
                text = stringResource(Res.string.report_history_count, session.counts.reviewCount.formatCount()),
                style = CaroTheme.typography.body2.semiBold,
                color = CaroTheme.color.text.primary,
            )
        }
        Text(
            text =
                stringResource(
                    Res.string.report_history_distribution,
                    stringResource(Res.string.learning_easy),
                    ratings.easy.formatCount(),
                    stringResource(Res.string.learning_fair),
                    ratings.fair.formatCount(),
                    stringResource(Res.string.learning_again),
                    ratings.again.formatCount(),
                ),
            style = CaroTheme.typography.caption1.regular,
            color = CaroTheme.color.text.secondary,
        )
    }
}

@Composable
private fun LocalDate.toHistoryDateText(): String =
    stringResource(Res.string.report_history_date, month.ordinal + 1, day, stringResource(dayOfWeek.label()))
