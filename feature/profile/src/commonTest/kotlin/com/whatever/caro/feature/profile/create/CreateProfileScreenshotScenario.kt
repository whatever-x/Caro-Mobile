package com.whatever.caro.feature.profile.create

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.profile.create.mvi.CreateProfileState
import com.whatever.caro.screenshot.ScreenshotScenario

internal object CreateProfileScreenshotScenario {
    const val ID = "profile.create.initial"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

    @Composable
    fun Render() {
        CaroTheme {
            CreateProfileScreen(state = CreateProfileState(), onIntent = {})
        }
    }
}
