package com.whatever.caro.feature.deck.component

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
import com.whatever.caro.core.designsystem.themes.CaroTheme

@Composable
internal fun CtaButton(
    enabled: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor =
        if (enabled) {
            CaroTheme.color.surface.brand
        } else {
            CaroTheme.color.surface.brand
                .copy(alpha = DISABLED_ALPHA)
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
            style = CaroTheme.typography.label1.bold,
            color = CaroTheme.color.text.inverse,
        )
    }
}
