package com.whatever.caro.core.designsystem.foundation

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal enum class CaroSpacingDefaults(
    val spacing: Dp
) {
    SPACING_XXS(spacing = 2.dp),
    SPACING_XS(spacing = 4.dp),
    SPACING_S(spacing = 8.dp),
    SPACING_M(spacing = 12.dp),
    SPACING_L(spacing = 16.dp),
    SPACING_XL(spacing = 20.dp),
    SPACING_2XL(spacing = 24.dp),
    SPACING_3XL(spacing = 32.dp),
    SPACING_4XL(spacing = 40.dp),
    SPACING_5XL(spacing = 48.dp),
    ;
}