package com.whatever.caro.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_logo
import caromobile.core.designsystem.generated.resources.ic_logo_apple
import caromobile.core.designsystem.generated.resources.ic_logo_google
import caromobile.core.designsystem.generated.resources.img_splash
import caromobile.core.designsystem.generated.resources.login_button_apple
import caromobile.core.designsystem.generated.resources.login_button_google
import caromobile.core.designsystem.generated.resources.login_text_bottom_terms_of_service
import caromobile.core.designsystem.generated.resources.login_text_logo_description
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.feature.login.component.SocialLoginButton
import com.whatever.caro.feature.login.mvi.LoginIntent
import com.whatever.caro.feature.login.mvi.LoginState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoginScreen(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    onLaunch: (SocialLoginType) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(color = CaroTheme.color.background.brand),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.img_splash),
                contentDescription = null,
            )
            Text(
                text = stringResource(resource = Res.string.login_text_logo_description),
                style = CaroTheme.typography.heading1,
                color = CaroTheme.color.text.inverse,
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CaroTheme.spacing.xl)
                    .padding(bottom = CaroTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(space = CaroTheme.spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SocialLoginButton(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(CaroTheme.shape.xxl)
                        .background(color = CaroTheme.color.surface.primary),
                iconRes = Res.drawable.ic_logo_google,
                contentRes = Res.string.login_button_google,
                textColor = CaroTheme.color.text.primary,
                onClick = { onLaunch(SocialLoginType.GOOGLE) },
            )

            SocialLoginButton(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(shape = CaroTheme.shape.xxl)
                        .background(color = CaroTheme.color.surface.inverse),
                iconRes = Res.drawable.ic_logo_apple,
                contentRes = Res.string.login_button_apple,
                textColor = CaroTheme.color.text.inverse,
                onClick = { onLaunch(SocialLoginType.APPLE) },
            )

            Text(
                text = stringResource(resource = Res.string.login_text_bottom_terms_of_service),
                style = CaroTheme.typography.caption1.regular,
                color = CaroTheme.color.text.tertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    CaroTheme {
        LoginScreen(
            state = LoginState(),
            onIntent = {},
            onLaunch = {},
        )
    }
}
