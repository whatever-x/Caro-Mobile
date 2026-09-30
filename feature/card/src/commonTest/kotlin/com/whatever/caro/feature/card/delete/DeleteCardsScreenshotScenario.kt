package com.whatever.caro.feature.card.delete

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.feature.card.delete.model.DeleteCardItem
import com.whatever.caro.feature.card.delete.mvi.DeleteCardsState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

internal object DeleteCardsScreenshotScenario {
    const val ID = "card.delete.selected"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            DeleteCardsScreen(
                state =
                    DeleteCardsState(
                        cards = persistentListOf(DeleteCardItem(DeckCard(1, CardContent("Apple", "사과"), CardBadge.NEW, 0))),
                        selectedCardIds = persistentSetOf(1L),
                    ),
                onIntent = {
                },
            )
        }
    }
}
