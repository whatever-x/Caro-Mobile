package com.whatever.caro.feature.deck.detail.components.lazycolumn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_detail_caption_review_count
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.detail.model.CardItem
import com.whatever.caro.feature.deck.detail.model.CardReviewState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DeckCardItem(
    card: CardItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val badgeText = card.reviewState.name

    val badgeTextColor =
        when (card.reviewState) {
            CardReviewState.HARD -> CaroTheme.color.badge.text.error
            CardReviewState.REVIEW -> CaroTheme.color.badge.text.info
            CardReviewState.NEW -> CaroTheme.color.badge.text.warning
        }

    val badgeBackgroundColor =
        when (card.reviewState) {
            CardReviewState.HARD -> CaroTheme.color.badge.surface.error
            CardReviewState.REVIEW -> CaroTheme.color.badge.surface.info
            CardReviewState.NEW -> CaroTheme.color.badge.surface.warning
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
                    color = CaroTheme.color.border.secondary,
                    shape = CaroTheme.shape.m,
                ).clip(shape = CaroTheme.shape.m)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = CaroTheme.spacing.xl,
                    vertical = CaroTheme.spacing.xl,
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
                style = CaroTheme.typography.heading2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = card.back,
                color = CaroTheme.color.text.secondary,
                style = CaroTheme.typography.label2,
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
                            shape = CaroTheme.shape.l,
                        ).padding(
                            horizontal = CaroTheme.spacing.s,
                            vertical = CaroTheme.spacing.xs,
                        ),
            ) {
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    style = CaroTheme.typography.caption1,
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
                    style = CaroTheme.typography.caption1,
                )
            }
        }
    }
}

@Preview
@Composable
private fun DeckCardItemPreview() {
    CaroTheme {
        Column(
            modifier =
                Modifier
                    .background(color = CaroTheme.color.background.primary)
                    .padding(CaroTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(CaroTheme.spacing.m),
        ) {
            CardReviewState.entries.forEach { reviewState ->
                DeckCardItem(
                    card =
                        CardItem(
                            front = "apple",
                            back = "사과",
                            reviewCount = 3,
                            reviewState = reviewState,
                        ),
                    onClick = { },
                )
            }
        }
    }
}
