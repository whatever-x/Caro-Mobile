package com.whatever.caro.feature.card

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.card.mvi.CreateCardState
import com.whatever.caro.screenshot.ScreenshotScenario

internal object CreateCardScreenshotScenario {
    const val ID = "card.create.draft"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

    @Composable
    fun Render() {
        CaroTheme {
            CreateCardScreen(state = CreateCardState(front = "Apple", back = "사과"), onIntent = {})
        }
    }
}
