package com.whatever.caro.feature.deck.edit

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.edit.mvi.EditDeckState

internal object EditDeckScreenshotScenario {
    const val ID = "deck.edit.existing"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            EditDeckScreen(state = EditDeckState(name = "Android", description = "기초 학습"), onIntent = {})
        }
    }
}
