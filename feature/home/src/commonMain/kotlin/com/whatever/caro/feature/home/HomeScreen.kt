package com.whatever.caro.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.home_banner_title
import caromobile.core.designsystem.generated.resources.home_floating_button
import caromobile.core.designsystem.generated.resources.home_learning_days
import caromobile.core.designsystem.generated.resources.ic_add_24
import caromobile.core.designsystem.generated.resources.ic_logo_small
import caromobile.core.designsystem.generated.resources.ic_setting_24
import caromobile.core.designsystem.generated.resources.img_fire
import caromobile.core.designsystem.generated.resources.img_home_banner
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.ui.noRippleClickable
import com.whatever.caro.feature.home.component.Deck
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    val scrollState = rememberScrollState()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(color = CaroTheme.color.background.primary),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            CaroTopBar(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(color = CaroTheme.color.background.brand)
                        .padding(horizontal = CaroTheme.spacing.xl2),
                leadingContent = {
                    Image(
                        painter = painterResource(resource = Res.drawable.ic_logo_small),
                        contentDescription = null,
                    )
                },
                trailingContent = {
                    Image(
                        painter = painterResource(resource = Res.drawable.ic_setting_24),
                        contentDescription = null,
                    )
                },
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(state = scrollState),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(color = CaroTheme.color.background.brand)
                            .padding(horizontal = CaroTheme.spacing.xl2)
                            .padding(bottom = 21.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(Res.string.home_banner_title, state.nickname),
                            style = CaroTheme.typography.display,
                            color = CaroTheme.color.text.inverse,
                        )
                        Spacer(modifier = Modifier.size(size = CaroTheme.spacing.s))
                        Text(
                            text = state.additionalDescription,
                            style = CaroTheme.typography.body3,
                        )
                        Spacer(modifier = Modifier.size(size = CaroTheme.spacing.l))
                        Row(
                            modifier =
                                Modifier
                                    .clip(
                                        shape = RoundedCornerShape(size = 50.dp),
                                    ).background(color = CaroTheme.color.overlay.light)
                                    .padding(
                                        horizontal = CaroTheme.spacing.m,
                                        vertical = CaroTheme.spacing.s,
                                    ),
                            horizontalArrangement = Arrangement.spacedBy(space = CaroTheme.spacing.xs),
                        ) {
                            Image(
                                modifier = Modifier.size(size = 18.dp),
                                painter = painterResource(resource = Res.drawable.img_fire),
                                contentDescription = null,
                            )
                            Text(
                                text =
                                    stringResource(
                                        Res.string.home_learning_days,
                                        state.learningDays,
                                    ),
                                style = CaroTheme.typography.body2.semiBold,
                                color = CaroTheme.color.text.warning,
                            )
                        }
                    }
                    Image(
                        painter = painterResource(resource = Res.drawable.img_home_banner),
                        contentDescription = null,
                    )
                }
            }
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CaroTheme.spacing.xl2),
                verticalArrangement = Arrangement.spacedBy(space = CaroTheme.spacing.m),
            ) {
                items(
                    items = state.decks,
                    key = { deck -> deck.id },
                ) {
                    Deck(
                        title = it.title,
                        description = it.description,
                        cardTotalCount = it.cardTotalCount,
                        todayLearningPercentage = it.todayProgress,
                        state = it.state,
                        onLearnClick = {
                            onIntent(HomeIntent.ClickDeckButton(deckId = it.id))
                        },
                    )
                }
            }
        }
        Row(
            modifier =
                Modifier
                    .align(alignment = Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .background(
                        shape = CaroTheme.shape.xxl,
                        color = CaroTheme.color.button.surface.floating,
                    ).padding(horizontal = CaroTheme.spacing.l, vertical = CaroTheme.spacing.m)
                    .noRippleClickable {
                        onIntent(HomeIntent.ClickCreateDeckButton)
                    },
            horizontalArrangement =
                Arrangement.spacedBy(
                    space = CaroTheme.spacing.s,
                    alignment = Alignment.CenterHorizontally,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(resource = Res.drawable.ic_add_24),
                tint = CaroTheme.color.icon.inverse,
                contentDescription = null,
            )

            Text(
                text = stringResource(resource = Res.string.home_floating_button),
                style = CaroTheme.typography.body2.semiBold,
                color = CaroTheme.color.text.inverse,
            )
        }

        Button(onClick = { onIntent(HomeIntent.ClickProfile) }) {
            Text("Profile")
        }

        Button(onClick = { onIntent(HomeIntent.ClickCreateDeck) }) {
            Text("덱 만들기")
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    val notStarted =
        Deck(
            id = 1,
            title = "Android",
            description = "기초 학습",
            cardTotalCount = 100,
            todayLearningCount = 10,
            todayCompleteCount = 0,
            state = DeckState.NOT_STARTED,
        )

    val learning =
        Deck(
            id = 2,
            title = "English",
            description = "단어 암기",
            cardTotalCount = 100,
            todayLearningCount = 10,
            todayCompleteCount = 7,
            state = DeckState.LEARNING,
        )

    val complete =
        Deck(
            id = 3,
            title = "Android",
            description = "기초 학습",
            cardTotalCount = 100,
            todayLearningCount = 100,
            todayCompleteCount = 100,
            state = DeckState.COMPLETE,
        )

    CaroTheme {
        HomeScreen(
            state =
                HomeState(
                    nickname = "승우",
                    additionalDescription = "화이또~",
                    learningDays = 10,
                    decks =
                        listOf(
                            notStarted,
                            learning,
                            complete,
                        ),
                ),
            onIntent = {},
        )
    }
}
