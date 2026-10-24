package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportLoading

internal object ImportLoadingScreenshotScenario {
    const val ID = "importer.parsing.loading"

    // 카드는 200ms 지연 뒤에 붙고 Lottie 로딩은 그 뒤에 시작하므로 지연을 넘긴 다음에 실제 시간으로 기다려야 한다.
    const val OVERLAY_DELAY_PASSED_MILLIS = 250L

    // 시계를 멈춘 채 실제 시간만 기다려 Lottie 파싱을 끝낸다. 이 구간에 시계를 움직이면 로딩이 끝나는 프레임에 따라 캡처가 달라진다.
    const val LOTTIE_LOAD_WAIT_MILLIS = 2_000L
    const val CAPTURE_CLOCK_MILLIS = 500L

    @Composable
    fun Render() {
        ImportScreenshotScenario.Render(ImportDeckState(loading = ImportLoading.Parsing(fileName = "JLPT N5 단어.apkg")))
    }
}
