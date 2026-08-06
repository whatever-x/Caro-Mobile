package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme

private val CaroDialogButtonHeight = 38.dp

/**
 * [CaroDialog] 의 `buttons` 슬롯에 배치하는 공용 버튼.
 *
 * 색상은 호출자가 지정한다. 예) 삭제 = surface.dangerous / text.dangerous,
 * 취소 = surface.tertiary / text.brand.
 */
@Composable
fun CaroDialogButton(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .height(CaroDialogButtonHeight)
                .clip(CaroTheme.shape.xxl)
                .background(backgroundColor)
                .noRippleClickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CaroTheme.typography.caption1.regular,
            color = textColor,
            textAlign = TextAlign.Center,
        )
    }
}
