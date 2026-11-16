package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme

/** 시각적 스위치. 클릭과 접근성 상태는 이를 포함하는 설정 행에서 제공한다. */
@Composable
fun CaroSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(width = 44.dp, height = 24.dp)
                .background(if (checked) CaroTheme.color.surface.brand else CaroTheme.color.border.secondary, CircleShape)
                .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(20.dp).background(CaroTheme.color.surface.primary, CircleShape))
    }
}
