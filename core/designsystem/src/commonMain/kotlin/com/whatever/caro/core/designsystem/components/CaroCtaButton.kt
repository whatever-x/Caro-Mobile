package com.whatever.caro.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.whatever.caro.core.designsystem.foundation.CaroCtaButtonHeight
import com.whatever.caro.core.designsystem.modifier.noRippleClickable
import com.whatever.caro.core.designsystem.themes.CaroTheme

/** Danger의 라벨 대비는 4.5:1에 못 미친다. 진한 위험 색 토큰이 생기기 전까지의 알려진 한계다. */
enum class CaroCtaButtonStyle {
    Brand,
    Danger,
}

/** [disabledReason]은 화면에 그리지 않고, 비활성일 때 접근성 stateDescription으로만 읽힌다. */
@Composable
fun CaroCtaButton(
    enabled: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CaroCtaButtonStyle = CaroCtaButtonStyle.Brand,
    disabledReason: String? = null,
) {
    val backgroundColor =
        when {
            !enabled -> CaroTheme.color.surface.disabled
            style == CaroCtaButtonStyle.Danger -> CaroTheme.color.surface.dangerous
            else -> CaroTheme.color.surface.brand
        }
    val textColor =
        when {
            !enabled -> CaroTheme.color.text.disabled
            style == CaroCtaButtonStyle.Danger -> CaroTheme.color.text.dangerous
            else -> CaroTheme.color.text.inverse
        }

    Box(
        modifier =
            modifier
                .heightIn(min = CaroCtaButtonHeight)
                .clip(CaroTheme.shape.xxl)
                .background(backgroundColor)
                .noRippleClickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics {
                    if (!enabled && disabledReason != null) {
                        stateDescription = disabledReason
                    }
                }.padding(
                    horizontal = CaroTheme.spacing.xl,
                    vertical = CaroTheme.spacing.l,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = CaroTheme.typography.label1,
            color = textColor,
        )
    }
}
