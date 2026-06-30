@file:Suppress("FunctionName")

package com.whatever.caro.feature.deck.detail.components.lazycolumn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_button_all_learning
import caromobile.core.designsystem.generated.resources.deck_detail_button_daily_learning
import caromobile.core.designsystem.generated.resources.deck_detail_caption_daily_learning_completed
import caromobile.core.designsystem.generated.resources.deck_detail_caption_daily_learning_in_progress
import caromobile.core.designsystem.generated.resources.deck_detail_caption_daily_learning_ready
import caromobile.core.designsystem.generated.resources.deck_detail_caption_daily_learning_unavailable
import caromobile.core.designsystem.generated.resources.deck_detail_label_daily_learning_completed
import caromobile.core.designsystem.generated.resources.deck_detail_label_daily_learning_in_progress
import caromobile.core.designsystem.generated.resources.deck_detail_label_daily_learning_unavailable
import caromobile.core.designsystem.generated.resources.deck_detail_sub_title_daily_learning
import caromobile.core.designsystem.generated.resources.deck_detail_title_daily_learning
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.DeckState
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.dailyLearningCard(
    learningCardCount: Int,
    learningCardTotal: Int,
    learningProgress: Int,
    currentLearningStatus: DeckState,
    onDailyStudy: () -> Unit,
    onAllStudy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    item {
        DailyLearningCardContent(
            counting = learningCardCount,
            total = learningCardTotal,
            learningProgress = learningProgress,
            learningStatus = currentLearningStatus,
            onDailyStudy = onDailyStudy,
            onAllStudy = onAllStudy,
            modifier =
                modifier
                    .padding(
                        start = CaroTheme.spacing.l,
                        end = CaroTheme.spacing.l,
                        bottom = CaroTheme.spacing.l,
                    ),
        )
    }
}

@Composable
private fun DailyLearningCardContent(
    counting: Int,
    total: Int,
    learningProgress: Int,
    learningStatus: DeckState,
    onDailyStudy: () -> Unit,
    onAllStudy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLearningUnavailable = total == 0

    val motivationalMessage =
        if (isLearningUnavailable) {
            stringResource(Res.string.deck_detail_caption_daily_learning_unavailable)
        } else {
            when (learningStatus) {
                DeckState.NOT_STARTED -> stringResource(Res.string.deck_detail_caption_daily_learning_ready)
                DeckState.LEARNING -> stringResource(Res.string.deck_detail_caption_daily_learning_in_progress)
                DeckState.COMPLETE -> stringResource(Res.string.deck_detail_caption_daily_learning_completed)
            }
        }

    val learningStatusLabelText =
        if (isLearningUnavailable) {
            stringResource(Res.string.deck_detail_label_daily_learning_unavailable)
        } else {
            when (learningStatus) {
                DeckState.NOT_STARTED -> ""
                DeckState.LEARNING -> stringResource(Res.string.deck_detail_label_daily_learning_in_progress)
                DeckState.COMPLETE -> stringResource(Res.string.deck_detail_label_daily_learning_completed)
            }
        }

    val learningStatusLabelTextColor =
        if (isLearningUnavailable) {
            CaroTheme.color.text.brand
        } else {
            when (learningStatus) {
                DeckState.LEARNING,
                DeckState.COMPLETE,
                -> CaroTheme.color.text.brand

                DeckState.NOT_STARTED -> Color.Unspecified
            }
        }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = CaroTheme.spacing.s),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(height = 211.dp)
                        .background(
                            color = CaroTheme.color.surface.primary,
                            shape = CaroTheme.shape.l,
                        ).border(
                            width = 1.dp,
                            color = CaroTheme.color.border.primary,
                            shape = CaroTheme.shape.l,
                        ).clip(shape = CaroTheme.shape.l)
                        .padding(all = CaroTheme.spacing.l),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(resource = Res.string.deck_detail_title_daily_learning),
                        color = CaroTheme.color.text.primary,
                        style = CaroTheme.typography.caption1,
                    )

                    Row(
                        modifier = Modifier.wrapContentHeight(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(size = 8.dp)
                                    .background(
                                        color = learningStatusLabelTextColor,
                                        shape = CircleShape,
                                    ),
                        )

                        Spacer(modifier = Modifier.width(width = CaroTheme.spacing.xs))

                        Text(
                            text = learningStatusLabelText,
                            color = learningStatusLabelTextColor,
                            style = CaroTheme.typography.caption1,
                        )
                    }
                }

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            space = CaroTheme.spacing.xs,
                            alignment = Alignment.CenterVertically,
                        ),
                ) {
                    Text(
                        text = stringResource(resource = Res.string.deck_detail_sub_title_daily_learning),
                        color = CaroTheme.color.text.secondary,
                        style = CaroTheme.typography.label1.bold,
                    )

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = counting.toString(),
                            color = CaroTheme.color.text.brand,
                            style = CaroTheme.typography.display,
                        )

                        Text(
                            text = "/$total",
                            color = CaroTheme.color.text.tertiary,
                            style = CaroTheme.typography.label1.bold,
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .dropShadow(
                                    shape = CaroTheme.shape.xxl,
                                    shadow =
                                        Shadow(
                                            radius = 3.dp,
                                            spread = 0.dp,
                                            color = Color.Black.copy(alpha = 0.1f),
                                            offset =
                                                DpOffset(
                                                    x = 0.dp,
                                                    y = 1.dp,
                                                ),
                                        ),
                                ).background(
                                    color = CaroTheme.color.surface.primary,
                                    shape = CaroTheme.shape.xxl,
                                ).border(
                                    width = 1.dp,
                                    color = CaroTheme.color.border.tertiary,
                                    shape = CaroTheme.shape.xxl,
                                ).padding(
                                    horizontal = CaroTheme.spacing.xl,
                                    vertical = 5.dp,
                                ),
                    ) {
                        Text(
                            text = motivationalMessage,
                            color = CaroTheme.color.text.tertiary,
                            style = CaroTheme.typography.caption2.bold,
                        )
                    }
                }

                Box( // TODO : CTA 버튼 컴포넌트 구현시 교체
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = CaroTheme.color.surface.brand,
                                shape = CaroTheme.shape.xxl,
                            ).clip(shape = CaroTheme.shape.xxl)
                            .clickable(onClick = onDailyStudy)
                            .padding(
                                horizontal = CaroTheme.spacing.l,
                                vertical = CaroTheme.spacing.m,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(resource = Res.string.deck_detail_button_daily_learning),
                        color = CaroTheme.color.text.inverse,
                        style = CaroTheme.typography.caption1,
                    )
                }
            }

            Text(
                modifier =
                    Modifier
                        .align(alignment = Alignment.TopEnd)
                        .offset(y = 35.dp),
                text = "$learningProgress%",
                color = CaroTheme.color.text.watermark,
                style = CaroTheme.typography.watermark,
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(shape = CaroTheme.shape.xxl)
                    .clickable(onClick = onAllStudy)
                    .padding(
                        horizontal = CaroTheme.spacing.l,
                        vertical = CaroTheme.spacing.m,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(resource = Res.string.deck_detail_button_all_learning),
                color = CaroTheme.color.text.primary,
                style = CaroTheme.typography.caption1,
            )
        }
    }
}

@Preview(locale = "ko")
@Preview(locale = "en")
@Composable
private fun DailyLearningCardContentPreview() {
    CaroTheme {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = 20.dp),
        ) {
            dailyLearningCard(
                learningCardCount = 0,
                learningCardTotal = 1000,
                learningProgress = 0,
                currentLearningStatus = DeckState.LEARNING,
                onDailyStudy = { },
                onAllStudy = { },
            )
        }
    }
}
