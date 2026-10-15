package com.whatever.caro.feature.card

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.card.mvi.EditCardState

internal object EditCardScreenshotScenario {
    const val ID = "card.edit.existing"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            EditCardScreen(state = EditCardState(front = "Apple", back = "사과"), onIntent = {})
        }
    }
}
