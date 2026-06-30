package com.whatever.caro.feature.deck.detail.components.lazycolumn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
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
import caromobile.core.designsystem.generated.resources.deck_detail_title_daily_learning
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
    val isLearningUnavailable = learningCardTotal == 0

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
        DailyLearningTitle(
            currentLearningStatus = currentLearningStatus,
            isLearningUnavailable = isLearningUnavailable,
        )

        Text(
            text = description,
            color = CaroTheme.color.text.secondary,
            style = CaroTheme.typography.label2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (isLearningUnavailable) {
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
    isLearningUnavailable: Boolean,
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

        DeckStateBadge(
            currentLearningStatus = currentLearningStatus,
            isLearningUnavailable = isLearningUnavailable,
        )
    }
}

@Composable
private fun DeckStateBadge(
    currentLearningStatus: DeckState,
    isLearningUnavailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val text =
        if (isLearningUnavailable) {
            stringResource(Res.string.deck_detail_label_daily_learning_unavailable)
        } else {
            when (currentLearningStatus) {
                DeckState.NOT_STARTED -> stringResource(Res.string.deck_detail_label_daily_learning_ready)
                DeckState.LEARNING -> stringResource(Res.string.deck_detail_label_daily_learning_in_progress)
                DeckState.COMPLETE -> stringResource(Res.string.deck_detail_label_daily_learning_completed)
            }
        }

    val textColor =
        if (isLearningUnavailable) {
            CaroTheme.color.text.error
        } else {
            when (currentLearningStatus) {
                DeckState.NOT_STARTED,
                DeckState.LEARNING,
                -> CaroTheme.color.text.brand

                DeckState.COMPLETE -> CaroTheme.color.text.secondary
            }
        }

    val backgroundColor =
        if (isLearningUnavailable) {
            CaroTheme.color.surface.warning
        } else {
            when (currentLearningStatus) {
                DeckState.NOT_STARTED,
                DeckState.LEARNING,
                -> CaroTheme.color.surface.info

                DeckState.COMPLETE -> CaroTheme.color.surface.primary
            }
        }

    val borderColor =
        when {
            isLearningUnavailable -> CaroTheme.color.border.warning
            currentLearningStatus == DeckState.COMPLETE -> CaroTheme.color.border.primary
            else -> CaroTheme.color.border.info
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
                text = "$learningProgress%",
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
                    color = CaroTheme.color.surface.warning,
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
                color = CaroTheme.color.text.error,
                style = CaroTheme.typography.label1.bold,
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
            color = Color.White,
            topLeft = cardOffset,
            size = cardSize,
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        drawRoundRect(
            color = Color(0xFFFFE494),
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
                .clickable(onClick = onClick),
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
