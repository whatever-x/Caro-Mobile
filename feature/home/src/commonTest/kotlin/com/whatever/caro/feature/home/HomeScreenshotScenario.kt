package com.whatever.caro.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.deck_title_create
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.feature.home.mvi.HomeState
import com.whatever.caro.feature.home.mvi.HomeStreakState
import kotlinx.collections.immutable.persistentListOf

internal object HomeScreenshotScenario {
    const val ID = "home.home.with_deck"

    fun scenarios() =
        listOf(
            ScreenshotScenario(
                ID,
                render = { Render() },
                expectedStrings = listOf(ScreenshotString(Res.string.deck_title_create)),
                beforeCapture = { strings ->
                    onNodeWithText(strings[Res.string.deck_title_create]).assertIsDisplayed()
                },
            ),
        )

    @Composable
    fun Render() {
        CaroTheme {
            HomeScreen(
                state =
                    HomeState(
                        nickname = "승우",
                        streakState = HomeStreakState.NotStarted,
                        decks = persistentListOf(Deck(1, "Android", "기초 학습", 10, 5, 0, DeckState.NOT_STARTED)),
                        isLoading = false,
                    ),
                onIntent = {
                },
            )
        }
    }
}
