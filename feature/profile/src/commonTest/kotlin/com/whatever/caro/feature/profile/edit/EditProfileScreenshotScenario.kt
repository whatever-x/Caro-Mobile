package com.whatever.caro.feature.profile.edit

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.profile.edit.mvi.EditProfileState
import com.whatever.caro.screenshot.ScreenshotScenario

internal object EditProfileScreenshotScenario {
    const val ID = "profile.edit.existing"

    fun scenarios() = listOf(ScreenshotScenario(ID, render = { Render() }))

    @Composable
    fun Render() {
        CaroTheme {
            EditProfileScreen(state = EditProfileState(nickname = "승우"), onIntent = {})
        }
    }
}
