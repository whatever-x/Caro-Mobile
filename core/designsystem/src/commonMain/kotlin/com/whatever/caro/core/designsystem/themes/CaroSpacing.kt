package com.whatever.caro.core.designsystem.themes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.whatever.caro.core.designsystem.foundation.CaroSpacingDefaults

@Immutable
data class CaroSpacing(
    val xxs: Dp,
    val xs: Dp,
    val s: Dp,
    val m: Dp,
    val l: Dp,
    val xl: Dp,
    val xl2: Dp,
    val xl2_2: Dp,
    val xl3: Dp,
    val xl4: Dp,
    val xl5: Dp,
) {
    companion object {
        fun defaultSpacing(): CaroSpacing =
            CaroSpacing(
                xxs = CaroSpacingDefaults.SPACING_2XS.spacing,
                xs = CaroSpacingDefaults.SPACING_XS.spacing,
                s = CaroSpacingDefaults.SPACING_S.spacing,
                m = CaroSpacingDefaults.SPACING_M.spacing,
                l = CaroSpacingDefaults.SPACING_L.spacing,
                xl = CaroSpacingDefaults.SPACING_XL.spacing,
                xl2 = CaroSpacingDefaults.SPACING_2XL.spacing,
                xl2_2 = CaroSpacingDefaults.SPACING_2XL_2.spacing,
                xl3 = CaroSpacingDefaults.SPACING_3XL.spacing,
                xl4 = CaroSpacingDefaults.SPACING_4XL.spacing,
                xl5 = CaroSpacingDefaults.SPACING_5XL.spacing,
            )
    }
}
