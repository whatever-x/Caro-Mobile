package com.whatever.caro.feature.deck.detail.components.lazycolumn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_button_all_learning
import caromobile.core.designsystem.generated.resources.deck_detail_button_daily_learning
import caromobile.core.designsystem.generated.resources.deck_detail_caption_daily_learning_unavailable_body_1
import caromobile.core.designsystem.generated.resources.deck_detail_caption_daily_learning_unavailable_body_2
import caromobile.core.designsystem.generated.resources.deck_detail_caption_daily_learning_unavailable_title
import caromobile.core.designsystem.generated.resources.deck_detail_label_daily_learning_completed
import caromobile.core.designsystem.generated.resources.deck_detail_label_daily_learning_in_progress
import caromobile.core.designsystem.generated.resources.deck_detail_label_daily_learning_ready
import caromobile.core.designsystem.generated.resources.deck_detail_label_daily_learning_unavailable
import caromobile.core.designsystem.generated.resources.deck_detail_label_learning_card_count
import caromobile.core.designsystem.generated.resources.deck_detail_label_learning_progress
import caromobile.core.designsystem.generated.resources.deck_detail_title_daily_learning
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.DeckState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeckDetailHeader(
    description: String,
    learningCardTotal: Int,
    learningProgress: Int,
    currentLearningStatus: DeckState,
    onDailyStudy: () -> Unit,
    onAllStudy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CaroTheme.spacing.xl,
                    vertical = CaroTheme.spacing.xl,
                ),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        DailyLearningTitle(currentLearningStatus = currentLearningStatus)
        Text(
            text = description,
            color = CaroTheme.color.text.secondary,
            style = CaroTheme.typography.label2,
        )

        if (currentLearningStatus == DeckState.REST_DAY) {
            UnavailableLearningMessage()
        } else {
            DailyLearningProgress(
                learningCardTotal = learningCardTotal,
                learningProgress = learningProgress,
            )

            if (currentLearningStatus != DeckState.COMPLETE) {
                DeckDetailButton(
                    text = stringResource(Res.string.deck_detail_button_daily_learning),
                    onClick = onDailyStudy,
                    isPrimary = true,
                )
            }
        }

        DeckDetailButton(
            text = stringResource(Res.string.deck_detail_button_all_learning),
            onClick = onAllStudy,
            isPrimary = false,
        )
    }
}

@Composable
private fun DailyLearningTitle(
    currentLearningStatus: DeckState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.deck_detail_title_daily_learning),
            modifier = Modifier.weight(1f),
            color = CaroTheme.color.text.primary,
            style = CaroTheme.typography.heading1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        DeckStateBadge(currentLearningStatus = currentLearningStatus)
    }
}

@Composable
private fun DeckStateBadge(
    currentLearningStatus: DeckState,
    modifier: Modifier = Modifier,
) {
    val text =
        when (currentLearningStatus) {
            DeckState.NOT_STARTED -> stringResource(Res.string.deck_detail_label_daily_learning_ready)
            DeckState.LEARNING -> stringResource(Res.string.deck_detail_label_daily_learning_in_progress)
            DeckState.COMPLETE -> stringResource(Res.string.deck_detail_label_daily_learning_completed)
            DeckState.REST_DAY -> stringResource(Res.string.deck_detail_label_daily_learning_unavailable)
        }

    val textColor =
        when (currentLearningStatus) {
            DeckState.NOT_STARTED, DeckState.LEARNING -> CaroTheme.color.text.brand
            DeckState.COMPLETE -> CaroTheme.color.text.secondary
            DeckState.REST_DAY -> CaroTheme.color.text.rest
        }

    val backgroundColor =
        when (currentLearningStatus) {
            DeckState.NOT_STARTED, DeckState.LEARNING -> CaroTheme.color.surface.ready
            DeckState.COMPLETE -> CaroTheme.color.surface.complete
            DeckState.REST_DAY -> CaroTheme.color.surface.rest
        }

    val borderColor =
        when (currentLearningStatus) {
            DeckState.NOT_STARTED, DeckState.LEARNING -> CaroTheme.color.border.ready
            DeckState.COMPLETE -> CaroTheme.color.border.complete
            DeckState.REST_DAY -> CaroTheme.color.border.rest
        }

    Box(
        modifier =
            modifier
                .background(
                    color = backgroundColor,
                    shape = CaroTheme.shape.xl,
                ).border(
                    width = 1.dp,
                    color = borderColor,
                    shape = CaroTheme.shape.xl,
                ).clip(CaroTheme.shape.xl)
                .padding(
                    horizontal = CaroTheme.spacing.s,
                    vertical = CaroTheme.spacing.xs,
                ),
    ) {
        Text(
            text = text,
            color = textColor,
            style = CaroTheme.typography.caption1.regular,
        )
    }
}

@Composable
private fun DailyLearningProgress(
    learningCardTotal: Int,
    learningProgress: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = CaroTheme.color.surface.tertiary,
                        shape = CaroTheme.shape.xs,
                    ),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction = learningProgress.coerceIn(0, 100) / 100f)
                        .height(8.dp)
                        .background(
                            color = CaroTheme.color.surface.brand,
                            shape = CaroTheme.shape.xs,
                        ),
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    stringResource(
                        resource = Res.string.deck_detail_label_learning_card_count,
                        learningCardTotal,
                    ),
                color = CaroTheme.color.text.tertiary,
                style = CaroTheme.typography.label2,
            )

            Box(
                modifier =
                    Modifier
                        .size(2.dp)
                        .background(
                            color = CaroTheme.color.text.tertiary,
                            shape = CaroTheme.shape.xxl,
                        ),
            )

            Text(
                text =
                    stringResource(
                        resource = Res.string.deck_detail_label_learning_progress,
                        learningProgress,
                    ),
                color = CaroTheme.color.text.brand,
                style = CaroTheme.typography.label2,
            )
        }
    }
}

@Composable
private fun UnavailableLearningMessage(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = CaroTheme.color.surface.rest,
                    shape = CaroTheme.shape.m,
                ).padding(CaroTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RestCardIcon()

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs),
        ) {
            Text(
                text = stringResource(Res.string.deck_detail_caption_daily_learning_unavailable_title),
                color = CaroTheme.color.text.rest,
                style = CaroTheme.typography.label1,
            )
            Text(
                text = stringResource(Res.string.deck_detail_caption_daily_learning_unavailable_body_1),
                color = CaroTheme.color.text.primary,
                style = CaroTheme.typography.label2,
            )
            Text(
                text = stringResource(Res.string.deck_detail_caption_daily_learning_unavailable_body_2),
                color = CaroTheme.color.text.primary,
                style = CaroTheme.typography.label2,
            )
        }
    }
}

@Composable
private fun RestCardIcon(modifier: Modifier = Modifier) {
    val starColor = CaroTheme.color.surface.brand
    val cardSurfaceColor = CaroTheme.color.surface.primary
    val cardBorderColor = CaroTheme.color.border.rest

    Canvas(
        modifier =
            modifier
                .size(54.dp)
                .rotate(-7f),
    ) {
        val cardSize = Size(width = 28.dp.toPx(), height = 38.dp.toPx())
        val cardOffset =
            Offset(
                x = (size.width - cardSize.width) / 2f,
                y = (size.height - cardSize.height) / 2f,
            )

        drawRoundRect(
            color = cardSurfaceColor,
            topLeft = cardOffset,
            size = cardSize,
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        drawRoundRect(
            color = cardBorderColor,
            topLeft = cardOffset,
            size = cardSize,
            cornerRadius = CornerRadius(4.dp.toPx()),
            style = Stroke(width = 1.dp.toPx()),
        )

        val center = Offset(size.width / 2f, size.height / 2f)
        val star = Path()
        star.moveTo(center.x, center.y - 7.dp.toPx())
        star.lineTo(center.x + 2.dp.toPx(), center.y - 2.dp.toPx())
        star.lineTo(center.x + 7.dp.toPx(), center.y)
        star.lineTo(center.x + 2.dp.toPx(), center.y + 2.dp.toPx())
        star.lineTo(center.x, center.y + 7.dp.toPx())
        star.lineTo(center.x - 2.dp.toPx(), center.y + 2.dp.toPx())
        star.lineTo(center.x - 7.dp.toPx(), center.y)
        star.lineTo(center.x - 2.dp.toPx(), center.y - 2.dp.toPx())
        star.close()
        drawPath(
            path = star,
            color = starColor,
        )
    }
}

@Composable
private fun DeckDetailButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    color =
                        if (isPrimary) {
                            CaroTheme.color.surface.brand
                        } else {
                            CaroTheme.color.surface.tertiary
                        },
                    shape = CaroTheme.shape.m,
                ).clip(CaroTheme.shape.m)
                .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color =
                if (isPrimary) {
                    CaroTheme.color.text.inverse
                } else {
                    CaroTheme.color.text.brand
                },
            style = CaroTheme.typography.heading2,
        )
    }
}
