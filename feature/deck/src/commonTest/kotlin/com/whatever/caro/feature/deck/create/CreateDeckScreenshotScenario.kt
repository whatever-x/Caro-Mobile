package com.whatever.caro.feature.deck.create

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.create.mvi.CreateDeckState
import com.whatever.caro.feature.deck.edit.CreateDeckScreen

internal object CreateDeckScreenshotScenario {
    const val ID = "deck.create.initial"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            CreateDeckScreen(state = CreateDeckState(), onIntent = {})
        }
    }
}
