package com.whatever.caro.core.ui.loading

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import caromobile.core.designsystem.generated.resources.Res
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kotlinx.coroutines.delay

private const val LOTTIE_SCREEN_LOADING_PATH = "files/lottie_screen_loading.json"
private const val SCREEN_LOADING_VISUAL_DELAY_MILLIS = 200L
private val ScreenLoadingSize = 90.dp

@Composable
fun CaroLoadingOverlayBox(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var isVisualOverlayVisible by remember(isLoading) { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            focusManager.clearFocus(force = true)
            delay(SCREEN_LOADING_VISUAL_DELAY_MILLIS)
            isVisualOverlayVisible = true
        }
    }

    if (isLoading) {
        val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = backState,
            onBackCompleted = {},
        )
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onPreviewKeyEvent { isLoading },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (isLoading) {
                            Modifier.clearAndSetSemantics { }
                        } else {
                            Modifier
                        },
                    ),
            content = content,
        )

        if (isLoading) {
            LoadingInteractionBlocker()
        }

        if (isVisualOverlayVisible) {
            CaroLoadingOverlay()
        }
    }
}

@Composable
private fun LoadingInteractionBlocker() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                },
    )
}

@Composable
fun CaroLoadingOverlay(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes(path = LOTTIE_SCREEN_LOADING_PATH).decodeToString(),
        )
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(color = CaroTheme.color.overlay.dim)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            modifier = Modifier.size(ScreenLoadingSize),
            painter =
                rememberLottiePainter(
                    composition = composition,
                    iterations = Compottie.IterateForever,
                ),
            contentDescription = null,
        )
    }
}
