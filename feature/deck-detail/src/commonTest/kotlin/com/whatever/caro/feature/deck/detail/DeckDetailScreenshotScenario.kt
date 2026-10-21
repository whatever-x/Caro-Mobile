package com.whatever.caro.feature.deck.detail

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.deck.Deck
import com.whatever.caro.core.model.deck.DeckState
import com.whatever.caro.feature.deck.detail.model.CardItem
import com.whatever.caro.feature.deck.detail.mvi.DeckDetailState
import kotlinx.collections.immutable.persistentListOf

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

    @Composable
    fun LibraryCopy() {
        CaroTheme {
            DeckDetailScreen(
                DeckDetailState(
                    deck = Deck(1, "토익 출제 단어 200개 (2)", "토익 준비를 위한 단어와 뜻", 200, 0, 0, DeckState.NOT_STARTED),
                    deckCardList =
                        persistentListOf(
                            CardItem(1, "apply", "지원하다"),
                            CardItem(2, "confirm", "확인하다"),
                            CardItem(3, "schedule", "일정"),
                            CardItem(4, "invoice", "청구서"),
                        ),
                ),
                onIntent = {},
            )
        }
    }
}
