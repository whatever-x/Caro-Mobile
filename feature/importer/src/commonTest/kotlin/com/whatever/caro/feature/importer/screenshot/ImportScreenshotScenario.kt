package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckState

/** 상태는 고정 리터럴만 쓴다. ViewModel, 파서, 시계에 기대면 같은 코드에서도 캡처가 달라진다. */
internal object ImportScreenshotScenario {
    const val WIDTH_DP = 393
    const val HEIGHT_DP = 852
    const val LOCALE = "ko"

    fun fileName(id: String): String = "$id.png"

    @Composable
    fun Render(state: ImportDeckState) {
        CaroTheme {
            ImportDeckScreen(state = state, onIntent = {})
        }
    }
}
