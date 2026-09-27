package com.whatever.caro.feature.card.detail

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.card.CardBadge
import com.whatever.caro.core.model.card.CardContent
import com.whatever.caro.core.model.card.DeckCard
import com.whatever.caro.feature.card.detail.mvi.CardDetailState
import kotlinx.collections.immutable.persistentListOf

internal object CardDetailScreenshotScenario {
    const val ID = "card.detail.front"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            CardDetailScreen(
                state =
                    CardDetailState(
                        cards = persistentListOf(DeckCard(1, CardContent("Apple", "사과"), CardBadge.NEW, 0)),
                    ),
                onIntent = {
                },
            )
        }
    }
}
