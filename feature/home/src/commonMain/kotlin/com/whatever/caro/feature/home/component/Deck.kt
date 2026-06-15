package com.whatever.caro.feature.home.component

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.home_deck_badge_complete
import caromobile.core.designsystem.generated.resources.home_deck_badge_learning
import caromobile.core.designsystem.generated.resources.home_deck_badge_not_started
import caromobile.core.designsystem.generated.resources.home_deck_button_learning
import caromobile.core.designsystem.generated.resources.home_deck_button_not_started
import caromobile.core.designsystem.generated.resources.ic_dot
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.ui.noRippleClickable
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
    onLearnClick: () -> Unit,
) {
    val progressSpace =
        when (state) {
            DeckState.NOT_STARTED, DeckState.LEARNING -> CaroTheme.spacing.m
            DeckState.COMPLETE -> CaroTheme.spacing.l
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape = CaroTheme.shape.xl)
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
        LinearProgressIndicator(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(height = 8.dp)
                    .clip(shape = CaroTheme.shape.xs),
            drawStopIndicator = {},
            progress = { todayLearningPercentage.coerceIn(0, 100).toFloat() / 100f },
            color = CaroTheme.color.surface.brand,
            trackColor = CaroTheme.color.surface.tertiary,
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
                    text = cardTotalCount.formatWithComma(),
                    style = CaroTheme.typography.body3,
                    color = CaroTheme.color.text.tertiary,
                )
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
            CtaButton(
                state = state,
                onClick = { onLearnClick() },
            )
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
            DeckState.COMPLETE -> return
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
            style = CaroTheme.typography.body1,
            color = CaroTheme.color.text.brand,
        )
    }
}

@Composable
private fun DeckStateBadge(
    modifier: Modifier = Modifier,
    state: DeckState,
) {
    // FIXME: 디자인 토큰 재적용 필요
    val backgroundColor =
        when (state) {
            DeckState.NOT_STARTED -> Color(0xFFFFEFCD)
            DeckState.LEARNING -> Color(0xFFC9D3FD)
            DeckState.COMPLETE -> Color(0xFFF8F8F9)
        }
    val borderColor =
        when (state) {
            DeckState.NOT_STARTED -> Color(0xFFFFEFCD)
            DeckState.LEARNING -> Color(0xFFC9D3FD)
            DeckState.COMPLETE -> Color(0xFFD8DADD)
        }
    val textColor =
        when (state) {
            DeckState.NOT_STARTED -> Color(0xFF8D6500)
            DeckState.LEARNING -> CaroTheme.color.text.brand
            DeckState.COMPLETE -> CaroTheme.color.text.secondary
        }
    val stringRes =
        when (state) {
            DeckState.NOT_STARTED -> Res.string.home_deck_badge_not_started
            DeckState.LEARNING -> Res.string.home_deck_badge_learning
            DeckState.COMPLETE -> Res.string.home_deck_badge_complete
        }

    Text(
        modifier =
            modifier
                .background(shape = RoundedCornerShape(size = 20.dp), color = backgroundColor)
                .padding(vertical = CaroTheme.spacing.xs, horizontal = CaroTheme.spacing.s)
                .border(width = 1.dp, color = borderColor),
        text = stringResource(stringRes),
        style = CaroTheme.typography.caption1,
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
            onLearnClick = {},
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
            onLearnClick = {},
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
            onLearnClick = {},
        )
    }
}
