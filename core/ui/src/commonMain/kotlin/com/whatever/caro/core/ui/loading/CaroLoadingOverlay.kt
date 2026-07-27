package com.whatever.caro.core.ui.loading

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import caromobile.core.designsystem.generated.resources.Res
import com.whatever.caro.core.designsystem.themes.CaroTheme
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

private const val LOTTIE_SCREEN_LOADING_PATH = "files/lottie_screen_loading.json"
private val ScreenLoadingSize = 90.dp

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
