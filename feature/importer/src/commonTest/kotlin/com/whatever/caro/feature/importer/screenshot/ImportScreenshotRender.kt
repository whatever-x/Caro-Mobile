package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import com.whatever.caro.core.designsystem.themes.CaroTheme
import com.whatever.caro.feature.importer.ImportDeckScreen
import com.whatever.caro.feature.importer.mvi.ImportDeckIntent
import com.whatever.caro.feature.importer.mvi.ImportDeckState

/** 상태는 고정 값만 쓴다. ViewModel, 파서, 시계에 기대면 같은 코드에서도 캡처가 달라진다. */
@Composable
internal fun RenderImportScreen(
    state: ImportDeckState,
    onIntent: (ImportDeckIntent) -> Unit = {},
) {
    CaroTheme {
        ImportDeckScreen(state = state, onIntent = onIntent)
    }
}

/**
 * 로딩 그림(Lottie)이 든 장면에 쓴다.
 * Lottie는 파일을 비동기로 읽어 캡처할 때 그림이 있을 때와 없을 때가 갈리므로, inspection mode로 그려 그림 자리를 비워 둔다.
 */
@Composable
internal fun RenderImportScreenWithoutLoadingAnimation(state: ImportDeckState) {
    CompositionLocalProvider(LocalInspectionMode provides true) {
        RenderImportScreen(state)
    }
}
