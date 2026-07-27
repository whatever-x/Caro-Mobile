package com.whatever.caro.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import caromobile.feature.splash.generated.resources.Res
import caromobile.feature.splash.generated.resources.img_splash
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.splash.mvi.SplashState
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun SplashScreen(state: SplashState) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CaroTheme.color.background.primary),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.img_splash),
            contentDescription = null,
            modifier = Modifier.size(80.dp),
        )
    }
}
