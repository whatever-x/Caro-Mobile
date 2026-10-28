package com.whatever.caro.feature.card

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.card.mvi.EditCardState
import com.whatever.caro.screenshot.ScreenshotScenario

internal object EditCardScreenshotScenario {
    const val ID = "card.edit.existing"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

    @Composable
    fun Render() {
        CaroTheme {
            EditCardScreen(state = EditCardState(front = "Apple", back = "사과"), onIntent = {})
        }
    }
}
