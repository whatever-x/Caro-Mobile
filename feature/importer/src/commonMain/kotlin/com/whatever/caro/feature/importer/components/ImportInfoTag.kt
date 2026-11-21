package com.whatever.caro.feature.importer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.whatever.caro.core.designsystem.themes.CaroTheme

@Composable
internal fun ImportInfoTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier =
            modifier
                .background(CaroTheme.color.surface.secondary, CaroTheme.shape.l)
                .padding(horizontal = CaroTheme.spacing.s, vertical = CaroTheme.spacing.xs),
        style = CaroTheme.typography.caption1.regular,
        color = CaroTheme.color.text.secondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
