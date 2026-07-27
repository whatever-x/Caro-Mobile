package com.whatever.caro.feature.profile.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

private val CtaButtonHeight = 56.dp
private val ButtonLoadingWidth = 57.dp
private val ButtonLoadingHeight = 17.dp
private const val LOTTIE_BUTTON_LOADING_PATH = "files/lottie_button_loading.json"
private const val CONTENT_TRANSITION_DURATION_MILLIS = 180
private const val CONTENT_TRANSITION_SCALE = 0.92f

/**
 * create/edit 화면이 공유하는 하단 CTA 버튼. 라벨 텍스트만 호출부에서 주입한다.
 */
@Composable
internal fun ProfileCtaButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor) =
        if (enabled || isLoading) {
            CaroTheme.color.surface.brand to CaroTheme.color.text.inverse
        } else {
            CaroTheme.color.surface.info to Color(0xFFA4B5FB)
        }

    Box(
        modifier =
            modifier
                .heightIn(min = CtaButtonHeight)
                .clip(CaroTheme.shape.xxl)
                .background(backgroundColor)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(
                    horizontal = CaroTheme.spacing.xl,
                    vertical = CaroTheme.spacing.l,
                ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                (
                    fadeIn(tween(CONTENT_TRANSITION_DURATION_MILLIS)) +
                        scaleIn(
                            initialScale = CONTENT_TRANSITION_SCALE,
                            animationSpec = tween(CONTENT_TRANSITION_DURATION_MILLIS),
                        )
                ).togetherWith(
                    fadeOut(tween(CONTENT_TRANSITION_DURATION_MILLIS)) +
                        scaleOut(
                            targetScale = CONTENT_TRANSITION_SCALE,
                            animationSpec = tween(CONTENT_TRANSITION_DURATION_MILLIS),
                        ),
                )
            },
            contentAlignment = Alignment.Center,
            label = "ProfileCtaLoadingTransition",
        ) { loading ->
            if (loading) {
                ProfileButtonLoading()
            } else {
                Text(
                    text = text,
                    style = CaroTheme.typography.label1.bold,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun ProfileButtonLoading() {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes(path = LOTTIE_BUTTON_LOADING_PATH).decodeToString(),
        )
    }

    Image(
        modifier = Modifier.size(width = ButtonLoadingWidth, height = ButtonLoadingHeight),
        painter =
            rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever,
            ),
        contentDescription = null,
    )
}
