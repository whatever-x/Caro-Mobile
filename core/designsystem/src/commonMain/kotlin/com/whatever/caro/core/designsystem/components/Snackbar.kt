package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
    override val actionLabel: String? = null,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false

    internal var dismissImmediately: Boolean = false
        private set

    internal fun markForImmediateDismissal() {
        dismissImmediately = true
    }
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
        dismissImmediately = ::shouldDismissSnackbarImmediately,
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

    Row(
        modifier =
            modifier
                .background(
                    color = backgroundColor,
                    shape = CaroTheme.shape.s,
                ).padding(
                    horizontal = CaroTheme.spacing.l,
                    vertical = CaroTheme.spacing.m,
                ),
        horizontalArrangement = Arrangement.spacedBy(CaroTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            style = CaroTheme.typography.body2.medium,
            text = snackbarData.visuals.message,
            textAlign = TextAlign.Center,
            color = textColor,
        )

        snackbarData.visuals.actionLabel?.let { actionLabel ->
            Text(
                modifier =
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = { performCaroSnackbarAction(snackbarData) },
                    ),
                text = actionLabel,
                style = CaroTheme.typography.label2.regular,
                color = CaroTheme.color.text.brand,
            )
        }
    }
}

internal fun performCaroSnackbarAction(snackbarData: SnackbarData) {
    (snackbarData.visuals as? CaroSnackbarVisuals)?.markForImmediateDismissal()
    snackbarData.performAction()
}

internal fun shouldDismissSnackbarImmediately(snackbarData: SnackbarData): Boolean =
    (snackbarData.visuals as? CaroSnackbarVisuals)?.dismissImmediately == true

fun showSnackbarMessage(
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    message: String,
    style: CaroSnackbarStyle = CaroSnackbarStyle.Normal,
    duration: SnackbarDuration = SnackbarDuration.Short,
    actionLabel: String? = null,
    onAction: (suspend () -> Unit)? = null,
) {
    coroutineScope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        val result =
            snackbarHostState.showSnackbar(
                CaroSnackbarVisuals(
                    message = message,
                    style = style,
                    duration = duration,
                    actionLabel = actionLabel,
                ),
            )
        if (result == SnackbarResult.ActionPerformed) {
            onAction?.invoke()
        }
    }
}
