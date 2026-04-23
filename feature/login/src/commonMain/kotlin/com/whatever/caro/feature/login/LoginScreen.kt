package com.whatever.caro.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import caromobile.core.designsystem.generated.resources.ic_logo
import caromobile.core.designsystem.generated.resources.ic_logo_apple
import caromobile.core.designsystem.generated.resources.ic_logo_google
import caromobile.core.designsystem.generated.resources.login_button_apple
import caromobile.core.designsystem.generated.resources.login_button_google
import caromobile.core.designsystem.generated.resources.login_text_bottom_terms_of_service
import caromobile.core.designsystem.generated.resources.login_text_logo_description
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.core.model.auth.SocialLoginType
import com.whatever.caro.feature.login.component.FlipCard
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFFFFFFFF)),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = 350.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF7FA8E8),
                                Color(0xFFAFC6F2),
                                Color(0xFFFFFFFF)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                FlipCard(
                    modifier = Modifier
                        .size(width = 300.dp, height = 180.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape =  CaroTheme.shape.xl,
                            ambientColor = Color(0x0000000),
                            spotColor = Color(0x40000000)
                        )
                        .background(
                            color = CaroTheme.color.surface.primary,
                            shape = CaroTheme.shape.xl,
                        )
                        .padding(vertical = 15.dp),
                    isFlipped = state.isFlip,
                    onClick = { onIntent(LoginIntent.ClickFlipCard) }
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(resource = Res.drawable.ic_logo),
                    contentDescription = null,
                )
                Text(
                    text = stringResource(resource = Res.string.login_text_logo_description),
                    style = CaroTheme.typography.heading2,
                    color = CaroTheme.color.text.secondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CaroTheme.spacing.xl)
                .padding(bottom = CaroTheme.spacing.l)
                .align(alignment = Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(space = CaroTheme.spacing.m)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFF000000), shape = CaroTheme.shape.xxl)
                    .background(
                        color = Color(0xFFFFFFFF),
                        shape = CaroTheme.shape.xxl
                    )
                    .padding(vertical = CaroTheme.spacing.xs)
                    .clickable {
                        onLaunch(SocialLoginType.GOOGLE)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.ic_logo_google),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(resource = Res.string.login_button_google),
                        style = CaroTheme.typography.robotoLabel1,
                        color = CaroTheme.color.text.primary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF000000),
                        shape = CaroTheme.shape.xxl,
                    )
                    .border(width = 1.dp, color = CaroTheme.color.border.secondary)
                    .padding(vertical = CaroTheme.spacing.xs)
                    .clickable {
                        onLaunch(SocialLoginType.GOOGLE)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_logo_apple),
                        contentDescription = null,
                        tint = Color.Unspecified,
                    )
                    Text(
                        text = stringResource(resource = Res.string.login_button_apple),
                        style = CaroTheme.typography.robotoLabel1,
                        color = Color(0xFFFFFFFF)
                    )
                }
            }

            Text(
                text = stringResource(resource = Res.string.login_text_bottom_terms_of_service),
                style = CaroTheme.typography.label1.regular,
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
            onLaunch = {}
        )
    }
}