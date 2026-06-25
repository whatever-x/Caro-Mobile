package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.whatever.caro.core.designsystem.animation.SlideInSlideOutSnackbarHost
import com.whatever.caro.core.designsystem.themes.CaroTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 토스트 시각 변형. 디자인 시스템의 surface/text 시맨틱 토큰에 매핑된다.
 * 팔레트에 success(녹색) 토큰이 없어 Normal/Info/Warning/Error 만 제공한다.
 */
enum class CaroSnackbarStyle {
    Normal,
    Info,
    Warning,
    Error,
}

/**
 * 스타일·표시 시간을 운반하는 커스텀 [SnackbarVisuals]. [CaroSnackbar] 가 style 을 읽어 색을 매핑한다.
 */
class CaroSnackbarVisuals(
    override val message: String,
    val style: CaroSnackbarStyle = CaroSnackbarStyle.Normal,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
}

/**
 * 스낵바를 제어하기 위한 호스트 컴포저블입니다.
 * @author GunHyung-Ham
 * @since 2025.03.31
 */
@Composable
fun CaroSnackBarHost(
    modifier: Modifier = Modifier,
    hostState: SnackbarHostState,
    snackbar: @Composable (SnackbarData) -> Unit,
) {
    val currentSnackbarData = hostState.currentSnackbarData

    LaunchedEffect(currentSnackbarData) {
        if (currentSnackbarData != null) {
            val duration =
                when (currentSnackbarData.visuals.duration) {
                    SnackbarDuration.Indefinite -> Long.MAX_VALUE
                    SnackbarDuration.Long -> 10000L
                    SnackbarDuration.Short -> 3000L
                }

            delay(duration)

            currentSnackbarData.dismiss()
        }
    }

    SlideInSlideOutSnackbarHost(
        current = hostState.currentSnackbarData,
        modifier = modifier,
        content = snackbar,
    )
}

@Composable
fun CaroSnackbar(
    modifier: Modifier = Modifier,
    snackbarData: SnackbarData,
) {
    val style =
        (snackbarData.visuals as? CaroSnackbarVisuals)?.style
            ?: CaroSnackbarStyle.Normal

    val backgroundColor =
        when (style) {
            CaroSnackbarStyle.Normal -> CaroTheme.color.surface.inverse
            CaroSnackbarStyle.Info -> CaroTheme.color.surface.info
            CaroSnackbarStyle.Warning -> CaroTheme.color.surface.warning
            CaroSnackbarStyle.Error -> CaroTheme.color.surface.error
        }

    val textColor =
        when (style) {
            CaroSnackbarStyle.Normal -> CaroTheme.color.text.inverse
            CaroSnackbarStyle.Info -> CaroTheme.color.text.info
            CaroSnackbarStyle.Warning -> CaroTheme.color.text.warning
            CaroSnackbarStyle.Error -> CaroTheme.color.text.error
        }

    Box(
        modifier =
            modifier
                .background(
                    color = backgroundColor,
                    shape = CaroTheme.shape.s,
                ).padding(
                    horizontal = CaroTheme.spacing.m,
                    vertical = CaroTheme.spacing.l,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            style = CaroTheme.typography.body3,
            text = snackbarData.visuals.message,
            textAlign = TextAlign.Center,
            color = textColor,
        )
    }
}

fun showSnackbarMessage(
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    message: String,
    style: CaroSnackbarStyle = CaroSnackbarStyle.Normal,
    duration: SnackbarDuration = SnackbarDuration.Short,
) {
    coroutineScope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
            CaroSnackbarVisuals(
                message = message,
                style = style,
                duration = duration,
            ),
        )
    }
}
