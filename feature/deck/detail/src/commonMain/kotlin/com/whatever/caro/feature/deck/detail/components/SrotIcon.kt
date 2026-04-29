package com.whatever.caro.feature.deck.detail.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

@Composable
internal fun SortIcon(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val scaleX = size.width / 15f
        val scaleY = size.height / 13.5f

        drawPath(
            path =
                Path().apply {
                    moveTo(x = 4f * scaleX, y = 5f * scaleY)
                    lineTo(x = 7.5f * scaleX, y = 8.5f * scaleY)
                    lineTo(x = 11f * scaleX, y = 5f * scaleY)
                    close()
                },
            color = color,
        )
    }
}
