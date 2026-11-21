package com.whatever.caro.feature.importer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whatever.caro.core.designsystem.themes.CaroTheme

private val HandleWidth = 40.dp
private val HandleHeight = 4.dp

@Composable
internal fun ImportSheetHandle() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = CaroTheme.color.surface.primary)
                .padding(vertical = CaroTheme.spacing.s),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .width(HandleWidth)
                    .height(HandleHeight)
                    .background(color = CaroTheme.color.divider.primary, shape = CaroTheme.shape.xxs),
        )
    }
}
