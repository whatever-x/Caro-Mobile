package com.whatever.caro.feature.profile.create

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.profile.create.mvi.CreateProfileState

internal object CreateProfileScreenshotScenario {
    const val ID = "profile.create.initial"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            CreateProfileScreen(state = CreateProfileState(), onIntent = {})
        }
    }
}
