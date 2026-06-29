package com.whatever.caro.feature.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme

private val CtaButtonHeight = 56.dp
private const val DISABLED_ALPHA = 0.4f

/**
 * create/edit 화면이 공유하는 하단 CTA 버튼. 라벨 텍스트만 호출부에서 주입한다.
 */
@Composable
internal fun ProfileCtaButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor) =
        if (enabled) {
            CaroTheme.color.surface.brand to CaroTheme.color.text.inverse
        } else {
            CaroTheme.color.badge.surface.info to Color(0xFFA4B5FB)
        }

    Box(
        modifier =
            modifier
                .heightIn(min = CtaButtonHeight)
                .clip(CaroTheme.shape.xxl)
                .background(backgroundColor)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(
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
