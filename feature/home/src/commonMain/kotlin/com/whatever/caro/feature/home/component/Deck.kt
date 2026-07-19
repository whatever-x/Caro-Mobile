package com.whatever.caro.feature.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.home_deck_badge_complete
import caromobile.core.designsystem.generated.resources.home_deck_badge_learning
import caromobile.core.designsystem.generated.resources.home_deck_badge_not_started
import caromobile.core.designsystem.generated.resources.home_deck_badge_rest
import caromobile.core.designsystem.generated.resources.home_deck_button_learning
import caromobile.core.designsystem.generated.resources.home_deck_button_not_started
import caromobile.core.designsystem.generated.resources.home_deck_card_total_count
import caromobile.core.designsystem.generated.resources.home_deck_rest_description1
import caromobile.core.designsystem.generated.resources.home_deck_rest_description2
import caromobile.core.designsystem.generated.resources.home_deck_rest_title
import caromobile.core.designsystem.generated.resources.ic_dot
import caromobile.core.designsystem.generated.resources.img_card
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.ui.modifier.noRippleClickable
import com.whatever.caro.core.util.NumberFormatter.formatWithComma
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Deck(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    cardTotalCount: Int,
    todayLearningPercentage: Int,
    state: DeckState,
    onDeckClick: () -> Unit,
) {
    val progressSpace =
        when (state) {
            DeckState.NOT_STARTED, DeckState.LEARNING -> CaroTheme.spacing.m
            DeckState.COMPLETE -> CaroTheme.spacing.l
            DeckState.REST_DAY -> CaroTheme.spacing.s
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape = CaroTheme.shape.xl)
                .noRippleClickable(onClick = onDeckClick)
                .background(color = CaroTheme.color.surface.primary)
                .border(width = 1.dp, color = CaroTheme.color.border.secondary)
                .padding(vertical = CaroTheme.spacing.xl, horizontal = CaroTheme.spacing.xl2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(weight = 1f),
                text = title,
                style = CaroTheme.typography.heading2,
                color = CaroTheme.color.text.primary,
                overflow = TextOverflow.Ellipsis,
            )
            DeckStateBadge(state = state)
        }
        Spacer(modifier = Modifier.size(size = CaroTheme.spacing.s))
        Text(
            text = description,
            style = CaroTheme.typography.body3,
            color = CaroTheme.color.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.size(size = progressSpace))
        ProgressContent(
            modifier = Modifier.fillMaxWidth(),
            todayLearningPercentage = todayLearningPercentage,
            deckState = state,
        )
        Spacer(modifier = Modifier.size(size = progressSpace))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(space = CaroTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        stringResource(
                            Res.string.home_deck_card_total_count,
                            cardTotalCount.formatWithComma(),
                        ),
                    style = CaroTheme.typography.body3,
                    color = CaroTheme.color.text.tertiary,
                )
                if (state != DeckState.REST_DAY) {
                    Icon(
                        painter = painterResource(resource = Res.drawable.ic_dot),
                        tint = CaroTheme.color.icon.tertiary,
                        contentDescription = null,
                    )
                    Text(
                        text = "$todayLearningPercentage%",
                        style = CaroTheme.typography.body2.semiBold,
                        color = CaroTheme.color.text.brand,
                    )
                }
            }
            CtaButton(
                state = state,
                onClick = onDeckClick,
            )
        }
    }
}

@Composable
private fun ProgressContent(
    modifier: Modifier = Modifier,
    deckState: DeckState,
    todayLearningPercentage: Int,
) {
    when (deckState) {
        DeckState.NOT_STARTED, DeckState.LEARNING, DeckState.COMPLETE -> {
            LinearProgressIndicator(
                modifier =
                    modifier
                        .height(height = 8.dp)
                        .clip(shape = CaroTheme.shape.xs),
                drawStopIndicator = {},
                progress = { todayLearningPercentage.coerceIn(0, 100).toFloat() / 100f },
                color = CaroTheme.color.surface.brand,
                trackColor = CaroTheme.color.surface.tertiary,
            )
        }

        DeckState.REST_DAY -> {
            Row(
                modifier =
                    modifier
                        .background(color = CaroTheme.color.surface.rest, shape = CaroTheme.shape.m)
                        .padding(all = CaroTheme.spacing.s),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        space = CaroTheme.spacing.s,
                        alignment = Alignment.CenterHorizontally,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(Res.drawable.img_card),
                    contentDescription = null,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(space = CaroTheme.spacing.xxs),
                ) {
                    Text(
                        text = stringResource(Res.string.home_deck_rest_title),
                        color = CaroTheme.color.text.rest,
                        style = CaroTheme.typography.caption2.bold,
                    )
                    Text(
                        text = stringResource(Res.string.home_deck_rest_description1),
                        color = CaroTheme.color.text.primary,
                        style = CaroTheme.typography.caption2.regular,
                    )
                    Text(
                        text = stringResource(Res.string.home_deck_rest_description2),
                        color = CaroTheme.color.text.primary,
                        style = CaroTheme.typography.caption2.regular,
                    )
                }
            }
        }
    }
}

@Composable
private fun CtaButton(
    state: DeckState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stringRes =
        when (state) {
            DeckState.NOT_STARTED -> Res.string.home_deck_button_not_started
            DeckState.LEARNING -> Res.string.home_deck_button_learning
            DeckState.COMPLETE, DeckState.REST_DAY -> return
        }
    Box(
        modifier =
            modifier
                .heightIn(min = 40.dp)
                .clip(CaroTheme.shape.m)
                .background(color = CaroTheme.color.surface.tertiary)
                .noRippleClickable(onClick = onClick)
                .padding(
                    horizontal = CaroTheme.spacing.l,
                    vertical = CaroTheme.spacing.s,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(resource = stringRes),
            style = CaroTheme.typography.body1.regular,
            color = CaroTheme.color.text.brand,
        )
    }
}

@Composable
private fun DeckStateBadge(
    modifier: Modifier = Modifier,
    state: DeckState,
) {
    val backgroundColor =
        when (state) {
            DeckState.NOT_STARTED, DeckState.LEARNING -> CaroTheme.color.surface.ready
            DeckState.COMPLETE -> CaroTheme.color.surface.complete
            DeckState.REST_DAY -> CaroTheme.color.surface.rest
        }
    val borderColor =
        when (state) {
            DeckState.NOT_STARTED, DeckState.LEARNING -> CaroTheme.color.border.ready
            DeckState.COMPLETE -> CaroTheme.color.border.complete
            DeckState.REST_DAY -> CaroTheme.color.border.rest
        }
    val textColor =
        when (state) {
            DeckState.NOT_STARTED, DeckState.LEARNING -> CaroTheme.color.text.ready
            DeckState.COMPLETE -> CaroTheme.color.text.complete
            DeckState.REST_DAY -> CaroTheme.color.text.rest
        }
    val stringRes =
        when (state) {
            DeckState.NOT_STARTED -> Res.string.home_deck_badge_not_started
            DeckState.LEARNING -> Res.string.home_deck_badge_learning
            DeckState.COMPLETE -> Res.string.home_deck_badge_complete
            DeckState.REST_DAY -> Res.string.home_deck_badge_rest
        }

    val badgeShape = RoundedCornerShape(size = 20.dp)

    Text(
        modifier =
            modifier
                .background(color = backgroundColor, shape = badgeShape)
                .border(width = 1.dp, color = borderColor, shape = badgeShape)
                .padding(vertical = CaroTheme.spacing.xs, horizontal = CaroTheme.spacing.s),
        text = stringResource(stringRes),
        style = CaroTheme.typography.caption1.regular,
        color = textColor,
    )
}

@Preview
@Composable
private fun DeckStateBadgePreview() {
    CaroTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 4.dp),
        ) {
            DeckState.entries.forEach {
                DeckStateBadge(state = it)
            }
        }
    }
}

@Preview
@Composable
private fun NotStartedDeckItemPreview() {
    CaroTheme {
        Deck(
            title = "제목1",
            description = "내용1",
            cardTotalCount = 1000,
            todayLearningPercentage = 0,
            state = DeckState.NOT_STARTED,
            onDeckClick = {},
        )
    }
}

@Preview
@Composable
private fun LearningDeckItemPreview() {
    CaroTheme {
        Deck(
            title = "제목1",
            description = "내용1",
            cardTotalCount = 1000,
            todayLearningPercentage = 70,
            state = DeckState.LEARNING,
            onDeckClick = {},
        )
    }
}

@Preview
@Composable
private fun CompleteDeckItemPreview() {
    CaroTheme {
        Deck(
            title = "제목1",
            description = "내용1",
            cardTotalCount = 1000,
            todayLearningPercentage = 100,
            state = DeckState.COMPLETE,
            onDeckClick = {},
        )
    }
}

@Preview
@Composable
private fun RestDeckItemPreview() {
    CaroTheme {
        Deck(
            title = "제목1",
            description = "내용1",
            cardTotalCount = 1000,
            todayLearningPercentage = 0,
            state = DeckState.REST_DAY,
            onDeckClick = {},
        )
    }
}
