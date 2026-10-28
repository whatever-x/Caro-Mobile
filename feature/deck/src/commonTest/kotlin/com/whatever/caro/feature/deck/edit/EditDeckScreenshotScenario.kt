package com.whatever.caro.feature.deck.edit

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.edit.mvi.EditDeckState
import com.whatever.caro.screenshot.ScreenshotScenario

internal object EditDeckScreenshotScenario {
    const val ID = "deck.edit.existing"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

    @Composable
    fun Render() {
        CaroTheme {
            EditDeckScreen(state = EditDeckState(name = "Android", description = "기초 학습"), onIntent = {})
        }
    }
}
