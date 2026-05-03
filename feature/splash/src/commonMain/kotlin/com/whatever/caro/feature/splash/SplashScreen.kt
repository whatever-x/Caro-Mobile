package com.whatever.caro.feature.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.splash.mvi.SplashState

@Composable
internal fun SplashScreen(state: SplashState) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CaroTheme.color.background.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Caro",
            style = CaroTheme.typography.display,
            color = CaroTheme.color.text.brand,
        )

        if (state.isInitializing) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = 48.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(
                    color = CaroTheme.color.icon.brand,
                )
            }
        }
    }
}
