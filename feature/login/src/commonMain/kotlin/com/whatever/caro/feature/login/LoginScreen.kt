package com.whatever.caro.feature.login

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.whatever.caro.feature.login.component.SocialLoginButton
import com.whatever.caro.feature.login.mvi.LoginIntent
import com.whatever.caro.feature.login.mvi.LoginState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LoginScreen(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    onLaunch: (SocialLoginType) -> Unit,
) {
    var isFlipped by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(initialValue = 0f) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(color = Color(0xFFFFFFFF)),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color(0xFF7FA8E8),
                                        Color(0xFFAFC6F2),
                                        Color(0xFFFFFFFF),
                                    ),
                            ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            FlipCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            this.rotationX = rotation.value
                            cameraDistance = 8 * density
                        }.padding(horizontal = 55.dp)
                        .heightIn(min = 180.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = CaroTheme.shape.xl,
                            ambientColor = Color(0x00000000),
                            spotColor = Color(0x40000000),
                        ).background(
                            color = CaroTheme.color.surface.primary,
                            shape = CaroTheme.shape.xl,
                        ).padding(vertical = 15.dp),
                isFlipped = isFlipped,
                onClick = {
                    if (isAnimating) return@FlipCard
                    isAnimating = true
                    coroutineScope.launch {
                        try {
                            val target = rotation.value + 180f
                            rotation.animateTo(
                                targetValue = rotation.value + 90f,
                                animationSpec = tween(200),
                            )
                            isFlipped = !isFlipped
                            rotation.animateTo(
                                targetValue = target,
                                animationSpec = tween(200),
                            )
                        } finally {
                            isAnimating = false
                        }
                    }
                },
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(2f),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .align(Alignment.TopCenter),
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
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CaroTheme.spacing.xl)
                        .padding(bottom = CaroTheme.spacing.l)
                        .align(alignment = Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(space = CaroTheme.spacing.m),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SocialLoginButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = Color(0xFF000000),
                                shape = CaroTheme.shape.xxl,
                            ).clip(CaroTheme.shape.xxl)
                            .background(color = Color(0xFFFFFFFF)),
                    iconRes = Res.drawable.ic_logo_google,
                    contentRes = Res.string.login_button_google,
                    textColor = Color(0xFF000000),
                    onClick = { onLaunch(SocialLoginType.GOOGLE) },
                )

                SocialLoginButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(shape = CaroTheme.shape.xxl)
                            .background(color = Color(0xFF000000)),
                    iconRes = Res.drawable.ic_logo_apple,
                    contentRes = Res.string.login_button_apple,
                    textColor = Color(0xFFFFFFFF),
                    onClick = { onLaunch(SocialLoginType.APPLE) },
                )

                Text(
                    text = stringResource(resource = Res.string.login_text_bottom_terms_of_service),
                    style = CaroTheme.typography.label1.regular,
                    color = CaroTheme.color.text.tertiary,
                    textAlign = TextAlign.Center,
                )
            }
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
