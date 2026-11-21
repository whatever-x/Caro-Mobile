package com.whatever.caro.feature.importer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.whatever.caro.core.designsystem.components.CaroCtaButton
import com.whatever.caro.core.designsystem.themes.CaroTheme

@Composable
internal fun ImportBottomBar(
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledReason: String? = null,
) {
    val gradient = Brush.verticalGradient(listOf(CaroTheme.color.gradient.tertiaryStart, CaroTheme.color.gradient.tertiaryEnd))

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(start = CaroTheme.spacing.xl2, end = CaroTheme.spacing.xl2, bottom = CaroTheme.spacing.l),
    ) {
        CaroCtaButton(
            text = buttonText,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            disabledReason = disabledReason,
        )
    }
}
