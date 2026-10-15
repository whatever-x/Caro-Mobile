package com.whatever.caro.feature.card

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.card.mvi.CreateCardState

internal object CreateCardScreenshotScenario {
    const val ID = "card.create.draft"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            CreateCardScreen(state = CreateCardState(front = "Apple", back = "사과"), onIntent = {})
        }
    }
}
