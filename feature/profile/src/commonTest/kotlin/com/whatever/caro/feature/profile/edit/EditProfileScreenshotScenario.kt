package com.whatever.caro.feature.profile.edit

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.profile.edit.mvi.EditProfileState

internal object EditProfileScreenshotScenario {
    const val ID = "profile.edit.existing"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            EditProfileScreen(state = EditProfileState(nickname = "승우"), onIntent = {})
        }
    }
}
