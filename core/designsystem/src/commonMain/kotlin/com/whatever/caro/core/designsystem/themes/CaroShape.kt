package com.whatever.caro.core.designsystem.themes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import com.whatever.caro.core.designsystem.foundation.CaroShapeDefaults

@Immutable
data class CaroShape(
    val xxs: Shape,
    val xs: Shape,
    val s: Shape,
    val m: Shape,
    val l: Shape,
    val xl: Shape,
    val xxl: Shape,
) {
    companion object {
        fun defaultShape(): CaroShape =
            CaroShape(
                xxs = CaroShapeDefaults.RADIUS_XXS.shape,
                xs = CaroShapeDefaults.RADIUS_XS.shape,
                s = CaroShapeDefaults.RADIUS_S.shape,
                m = CaroShapeDefaults.RADIUS_M.shape,
                l = CaroShapeDefaults.RADIUS_L.shape,
                xl = CaroShapeDefaults.RADIUS_XL.shape,
                xxl = CaroShapeDefaults.RADIUS_XXL.shape,
            )
    }
}
