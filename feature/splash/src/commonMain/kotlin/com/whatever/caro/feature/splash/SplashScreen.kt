package com.whatever.caro.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.img_splash
import caromobile.core.designsystem.generated.resources.splash_content
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.splash.mvi.SplashState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SplashScreen(state: SplashState) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CaroTheme.color.background.brand),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.img_splash),
            contentDescription = null,
        )
        Text(
            text = stringResource(Res.string.splash_content),
            style = CaroTheme.typography.heading1,
            color = CaroTheme.color.text.inverse,
            textAlign = TextAlign.Center,
        )
    }
}
