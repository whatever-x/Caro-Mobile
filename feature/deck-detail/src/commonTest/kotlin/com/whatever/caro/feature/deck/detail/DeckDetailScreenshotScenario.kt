package com.whatever.caro.feature.deck.detail

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState

internal object DeckDetailScreenshotScenario {
    const val ID = "deck_detail.detail.empty"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            DeckDetailScreen(state = DeckDetailState(deck = Deck(1, "Android", "기초 학습", 0, 0, 0, DeckState.NOT_STARTED)), onIntent = {})
        }
    }
}
