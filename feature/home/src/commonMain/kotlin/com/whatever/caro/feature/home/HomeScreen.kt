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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.home_banner_title
import caromobile.core.designsystem.generated.resources.home_deck_field_empty
import caromobile.core.designsystem.generated.resources.home_floating_button
import caromobile.core.designsystem.generated.resources.home_load_error
import caromobile.core.designsystem.generated.resources.home_retry
import caromobile.core.designsystem.generated.resources.home_streak_active_description
import caromobile.core.designsystem.generated.resources.home_streak_active_label
import caromobile.core.designsystem.generated.resources.home_streak_broken_description
import caromobile.core.designsystem.generated.resources.home_streak_broken_label
import caromobile.core.designsystem.generated.resources.home_streak_not_started_description
import caromobile.core.designsystem.generated.resources.home_streak_not_started_label
import caromobile.core.designsystem.generated.resources.ic_add_24
import caromobile.core.designsystem.generated.resources.ic_logo_small
import caromobile.core.designsystem.generated.resources.ic_setting_24
import caromobile.core.designsystem.generated.resources.img_fire
import caromobile.core.designsystem.generated.resources.img_home_streak_active
import caromobile.core.designsystem.generated.resources.img_home_streak_broken
import caromobile.core.designsystem.generated.resources.img_home_streak_not_started
import caromobile.core.designsystem.generated.resources.img_streak_broken
import com.whatever.caro.core.designsystem.components.CaroTopBar
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.core.ui.loading.CaroLoadingOverlayBox
import com.whatever.caro.feature.home.component.Deck
import com.whatever.caro.feature.home.mvi.HomeIntent
import com.whatever.caro.feature.home.mvi.HomeState
import com.whatever.caro.feature.home.mvi.HomeStreakState
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.ExperimentalCompottieApi
import io.github.alexzhirkevich.compottie.Lottie
import io.github.alexzhirkevich.compottie.LottieClipSpec
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.Resource
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val ARROW_BOUNCE_LOOP_END_PROGRESS = 35f / 60f
private val HomeLoadingBannerHeight = 128.dp
private val ArrowBounceSize = 50.dp

@OptIn(ExperimentalResourceApi::class, ExperimentalCompottieApi::class)
@Composable
internal fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
) {
    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.Resource(Res.getUri("files/arrow_down_bounce.json")),
    )

    CaroLoadingOverlayBox(
        isLoading = !state.isLoadedContentVisible,
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
                        modifier = Modifier.noRippleClickable { onIntent(HomeIntent.ClickSettingButton) },
                        painter = painterResource(resource = Res.drawable.ic_setting_24),
                        contentDescription = null,
                    )
                },
            )

            if (state.hasLoadError) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(CaroTheme.color.surface.error)
                            .padding(
                                horizontal = CaroTheme.spacing.xl2,
                                vertical = CaroTheme.spacing.m,
                            ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(Res.string.home_load_error),
                        style = CaroTheme.typography.label1,
                        color = CaroTheme.color.text.primary,
                    )
                    Text(
                        modifier =
                            Modifier
                                .padding(start = CaroTheme.spacing.m)
                                .noRippleClickable { onIntent(HomeIntent.ClickRetry) },
                        text = stringResource(Res.string.home_retry),
                        style = CaroTheme.typography.label1,
                        color = CaroTheme.color.text.brand,
                    )
                }
            }

            if (state.isLoadedContentVisible) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    overscrollEffect = null,
                ) {
                    item {
                        HomeStreakBanner(state = state)
                    }

                    itemsIndexed(
                        items = state.decks,
                        key = { _, deck -> deck.id },
                    ) { index, deck ->
                        Spacer(modifier = Modifier.size(size = CaroTheme.spacing.m))
                        Deck(
                            modifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2),
                            title = deck.title,
                            description = deck.description,
                            cardTotalCount = deck.cardTotalCount,
                            todayLearningPercentage = deck.todayProgress,
                            state = deck.state,
                            onDeckClick = {
                                onIntent(
                                    HomeIntent.ClickDeckButton(
                                        deck = deck,
                                    ),
                                )
                            },
                            onStartLearningClick = {
                                onIntent(
                                    HomeIntent.ClickStartLearning(
                                        deckId = deck.id,
                                    ),
                                )
                            },
                        )
                        if (index == state.decks.lastIndex) {
                            Spacer(modifier = Modifier.size(size = 72.dp + CaroTheme.spacing.l))
                        }
                    }
                }
            } else {
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(HomeLoadingBannerHeight)
                            .background(color = CaroTheme.color.background.brand),
                )
            }
        }
        if (state.isLoadedContentVisible && state.isDeckEmpty) {
            Text(
                modifier =
                    Modifier
                        .align(alignment = Alignment.Center)
                        .padding(horizontal = CaroTheme.spacing.xl2),
                text = stringResource(resource = Res.string.home_deck_field_empty),
                style = CaroTheme.typography.heading3,
                color = CaroTheme.color.text.primary,
                textAlign = TextAlign.Center,
            )
        }
        if (state.isLoadedContentVisible) {
            Column(
                modifier =
                    Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.isDeckEmpty) {
                    Lottie(
                        modifier = Modifier.size(size = ArrowBounceSize),
                        painter =
                            rememberLottiePainter(
                                composition = lottieComposition,
                                iterations = Compottie.IterateForever,
                                clipSpec = LottieClipSpec.Progress(0f, ARROW_BOUNCE_LOOP_END_PROGRESS),
                            ),
                        contentDescription = null,
                    )
                }
                Row(
                    modifier =
                        Modifier
                            .background(
                                shape = CaroTheme.shape.xxl,
                                color = CaroTheme.color.surface.brand,
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
            }
        }
    }
}

@Composable
private fun HomeStreakBanner(state: HomeState) {
    val content = state.streakState.toBannerContent()

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = CaroTheme.color.background.brand)
                .padding(horizontal = CaroTheme.spacing.xl2)
                .padding(bottom = CaroTheme.spacing.xl2)
                .heightIn(min = 109.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.home_banner_title, state.nickname),
                style = CaroTheme.typography.display,
                color = CaroTheme.color.text.inverse,
            )
            content?.let {
                Spacer(modifier = Modifier.size(CaroTheme.spacing.s))
                Text(
                    text = it.description,
                    style = CaroTheme.typography.body2.medium,
                    color = CaroTheme.color.text.tertiary,
                )
                Spacer(modifier = Modifier.size(CaroTheme.spacing.m))
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(CaroTheme.color.overlay.light)
                            .padding(
                                horizontal = CaroTheme.spacing.m,
                                vertical = CaroTheme.spacing.xs,
                            ),
                    horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        modifier = Modifier.size(18.dp),
                        painter = painterResource(it.icon),
                        contentDescription = null,
                    )
                    Text(
                        text = it.label,
                        style = CaroTheme.typography.body2.semiBold,
                        color = it.labelColor,
                    )
                }
            }
        }
        content?.let {
            Image(
                modifier = Modifier.size(88.dp),
                painter = painterResource(it.character),
                contentDescription = null,
            )
        }
    }
}

private data class HomeStreakBannerContent(
    val description: String,
    val label: String,
    val icon: DrawableResource,
    val character: DrawableResource,
    val labelColor: Color,
)

@Composable
private fun HomeStreakState.toBannerContent(): HomeStreakBannerContent? =
    when (this) {
        HomeStreakState.Loading -> {
            null
        }

        HomeStreakState.NotStarted -> {
            HomeStreakBannerContent(
                description = stringResource(Res.string.home_streak_not_started_description),
                label = stringResource(Res.string.home_streak_not_started_label),
                icon = Res.drawable.img_fire,
                character = Res.drawable.img_home_streak_not_started,
                labelColor = CaroTheme.color.text.warning,
            )
        }

        is HomeStreakState.Active -> {
            HomeStreakBannerContent(
                description = stringResource(Res.string.home_streak_active_description, days),
                label = stringResource(Res.string.home_streak_active_label, days),
                icon = Res.drawable.img_fire,
                character = Res.drawable.img_home_streak_active,
                labelColor = CaroTheme.color.text.warning,
            )
        }

        HomeStreakState.Broken -> {
            HomeStreakBannerContent(
                description = stringResource(Res.string.home_streak_broken_description),
                label = stringResource(Res.string.home_streak_broken_label),
                icon = Res.drawable.img_streak_broken,
                character = Res.drawable.img_home_streak_broken,
                labelColor = CaroTheme.color.text.tertiary,
            )
        }
    }

@Preview
@Composable
private fun HomeScreenActivePreview() {
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
                    streakState = HomeStreakState.Active(days = 10),
                    decks =
                        persistentListOf(
                            notStarted,
                            learning,
                            complete,
                        ),
                ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun HomeScreenNotStartedWithoutDeckPreview() {
    CaroTheme {
        HomeScreen(
            state =
                HomeState(
                    nickname = "승우",
                    streakState = HomeStreakState.NotStarted,
                ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun HomeScreenNotStartedWithFirstDeckPreview() {
    CaroTheme {
        HomeScreen(
            state =
                HomeState(
                    nickname = "승우",
                    streakState = HomeStreakState.NotStarted,
                    decks =
                        persistentListOf(
                            Deck(
                                id = 1,
                                title = "Android",
                                description = "기초 학습",
                                cardTotalCount = 100,
                                todayLearningCount = 10,
                                todayCompleteCount = 0,
                                state = DeckState.NOT_STARTED,
                            ),
                        ),
                ),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun HomeScreenBrokenPreview() {
    CaroTheme {
        HomeScreen(
            state =
                HomeState(
                    nickname = "승우",
                    streakState = HomeStreakState.Broken,
                    decks =
                        persistentListOf(
                            Deck(
                                id = 1,
                                title = "Android",
                                description = "기초 학습",
                                cardTotalCount = 100,
                                todayLearningCount = 10,
                                todayCompleteCount = 0,
                                state = DeckState.NOT_STARTED,
                            ),
                        ),
                ),
            onIntent = {},
        )
    }
}
