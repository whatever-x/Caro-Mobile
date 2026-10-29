package com.whatever.caro.feature.deck.create

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.deck.ScreenshotScenario
import com.whatever.caro.feature.deck.create.mvi.CreateDeckState
import com.whatever.caro.feature.deck.edit.CreateDeckScreen

internal object CreateDeckScreenshotScenario {
    const val ID = "deck.create.initial"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

    @Composable
    fun Render() {
        CaroTheme {
            CreateDeckScreen(state = CreateDeckState(), onIntent = {})
        }
    }
}
