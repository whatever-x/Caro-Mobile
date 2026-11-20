package com.whatever.caro.feature.importer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.components.CaroCheckbox
import com.whatever.caro.core.designsystem.themes.CaroTheme

private val OffBorderWidth = 1.5.dp

/**
 * 공용 [CaroCheckbox]의 꺼진 모양은 테두리 색이 화면 바탕과 거의 같아 가져오기 화면에서 보이지 않는다.
 * 가져오기 시안의 꺼진 체크박스(흰 바탕, 회색 테두리 1.5)를 겹쳐 그린다. 상태 semantics는 부모 줄이 맡는다.
 */
@Composable
internal fun ImportCheckbox(state: ToggleableState) {
    val shape = CaroTheme.shape.xs
    CaroCheckbox(
        state = state,
        modifier =
            if (state == ToggleableState.Off) {
                Modifier
                    .background(CaroTheme.color.surface.primary, shape)
                    .border(OffBorderWidth, CaroTheme.color.border.secondary, shape)
            } else {
                Modifier
            },
    )
}
