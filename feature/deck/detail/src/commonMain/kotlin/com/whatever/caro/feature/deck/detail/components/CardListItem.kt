package com.whatever.caro.feature.deck.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_button_add_card
import caromobile.core.designsystem.generated.resources.deck_detail_caption_review_count
import caromobile.core.designsystem.generated.resources.ic_add_16
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.detail.extension.dashedBorder
import com.whatever.caro.feature.deck.detail.model.CardItem
import com.whatever.caro.feature.deck.detail.model.CardReviewState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun AddCardButtonItem(
    onAddCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = CaroTheme.spacing.l),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color = CaroTheme.color.surface.primary,
                        shape = CaroTheme.shape.m,
                    ).dashedBorder(
                        color = CaroTheme.color.border.primary,
                        radius = CaroTheme.spacing.m,
                    ).clip(shape = CaroTheme.shape.m)
                    .clickable(onClick = onAddCard)
                    .padding(
                        horizontal = CaroTheme.spacing.l,
                        vertical = CaroTheme.spacing.m,
                    ),
            horizontalArrangement =
                Arrangement.spacedBy(
                    space = CaroTheme.spacing.xs,
                    alignment = Alignment.CenterHorizontally,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_add_16),
                contentDescription = null,
                tint = CaroTheme.color.icon.primary,
            )

            Text(
                text = stringResource(Res.string.deck_detail_button_add_card),
                color = CaroTheme.color.text.primary,
                style = CaroTheme.typography.caption1,
            )
        }
    }
}

@Composable
internal fun DeckCardItem(
    card: CardItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeText = card.reviewState.name

    val badgeTextColor =
        when (card.reviewState) {
            CardReviewState.HARD -> CaroTheme.color.text.error
            CardReviewState.REVIEW -> CaroTheme.color.text.info
            CardReviewState.NEW -> CaroTheme.color.text.warning
        }

    val badgeBackgroundColor =
        when (card.reviewState) {
            CardReviewState.HARD -> CaroTheme.color.surface.error
            CardReviewState.REVIEW -> CaroTheme.color.surface.info
            CardReviewState.NEW -> CaroTheme.color.surface.warning
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = CaroTheme.color.surface.primary,
                    shape = CaroTheme.shape.m,
                ).border(
                    width = 1.dp,
                    color = CaroTheme.color.border.primary,
                    shape = CaroTheme.shape.m,
                ).clip(shape = CaroTheme.shape.m)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = CaroTheme.spacing.l,
                    vertical = CaroTheme.spacing.m,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xxs),
        ) {
            Text(
                text = card.front,
                color = CaroTheme.color.text.primary,
                style = CaroTheme.typography.heading3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = card.back,
                color = CaroTheme.color.text.secondary,
                style = CaroTheme.typography.body2.regular,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
        ) {
            Box(
                modifier =
                    Modifier
                        .background(
                            color = badgeBackgroundColor,
                            shape = CaroTheme.shape.s,
                        ).padding(
                            horizontal = CaroTheme.spacing.s,
                            vertical = CaroTheme.spacing.xxs,
                        ),
            ) {
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    style = CaroTheme.typography.caption2.bold,
                )
            }

            if (card.reviewCount != 0) {
                Text(
                    text =
                        stringResource(
                            Res.string.deck_detail_caption_review_count,
                            card.reviewCount,
                        ),
                    color = CaroTheme.color.text.tertiary,
                    style = CaroTheme.typography.caption2.bold,
                )
            }
        }
    }
}
