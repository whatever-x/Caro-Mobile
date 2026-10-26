package com.whatever.caro.feature.setting

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.feature.setting.mvi.SettingState

internal object SettingScreenshotScenario {
    const val ID = "setting.setting.account"
    const val FILE_NAME = "$ID.png"
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    @Composable
    fun Render() {
        CaroTheme {
            SettingScreen(
                state =
                    SettingState(
                        isLoading = false,
                        nickname = "승우",
                        emailAddress = "caro@example.com",
                        socialLoginType = SocialLoginType.GOOGLE,
                    ),
                appVersion = "1.0.0",
                onIntent = {
                },
            )
        }
    }
}
