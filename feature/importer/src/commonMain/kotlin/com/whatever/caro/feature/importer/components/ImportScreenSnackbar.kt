package com.whatever.caro.feature.importer.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import com.whatever.caro.core.designsystem.components.CaroSnackBarHost
import com.whatever.caro.core.designsystem.components.CaroSnackbar
import com.whatever.caro.core.designsystem.themes.CaroTheme
import kotlin.math.min

/** 하단 버튼의 위 끝이다. 버튼을 감싼 레이아웃을 거쳐 화면까지 올라온다. */
private val ImportBottomButtonTop = HorizontalAlignmentLine(::min)

internal fun Modifier.importBottomButtonTop(): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height, alignmentLines = mapOf(ImportBottomButtonTop to 0)) { placeable.place(0, 0) }
    }

/**
 * 전역 스낵바 호스트는 홈에만 하단 여백을 주므로 가져오기 화면은 이 안에서 스낵바를 띄운다.
 * 하단 영역은 사유 줄과 큰 글자로 높이가 바뀌어 고정 여백을 쓸 수 없고, 하단 버튼이 알린 위 끝에서 띄운다. 하단 버튼이 없는 단계는 화면 아래 끝에서 띄운다.
 */
@Composable
internal fun ImportSnackbarFrame(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val gap = CaroTheme.spacing.l
    val snackbar: @Composable () -> Unit = {
        CaroSnackBarHost(
            modifier = Modifier.padding(horizontal = CaroTheme.spacing.xl2),
            hostState = hostState,
            snackbar = { data -> CaroSnackbar(snackbarData = data) },
        )
    }

    Layout(contents = listOf(content, snackbar), modifier = modifier) { (contentMeasurables, snackbarMeasurables), constraints ->
        val body = contentMeasurables.single().measure(constraints)
        val host = snackbarMeasurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val gapPx = gap.roundToPx()
        layout(body.width, body.height) {
            body.place(0, 0)
            val buttonTop = body[ImportBottomButtonTop].takeIf { line -> line != AlignmentLine.Unspecified } ?: body.height
            host.place((body.width - host.width) / 2, buttonTop - gapPx - host.height)
        }
    }
}
