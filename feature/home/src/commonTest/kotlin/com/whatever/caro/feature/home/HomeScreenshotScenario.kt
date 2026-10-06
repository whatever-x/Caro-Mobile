package com.whatever.caro.feature.home

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.feature.home.mvi.HomeState
import com.whatever.caro.feature.home.mvi.HomeStreakState
import kotlinx.collections.immutable.persistentListOf

internal object HomeScreenshotScenario {
    const val ID = "home.home.with_deck"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

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
