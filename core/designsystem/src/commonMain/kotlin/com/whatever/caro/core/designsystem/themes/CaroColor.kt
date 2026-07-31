package com.whatever.caro.core.designsystem.themes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.whatever.caro.core.designsystem.foundation.Background100
import com.whatever.caro.core.designsystem.foundation.Black100
import com.whatever.caro.core.designsystem.foundation.BlackAlpha20
import com.whatever.caro.core.designsystem.foundation.BlackAlpha60
import com.whatever.caro.core.designsystem.foundation.Blue100
import com.whatever.caro.core.designsystem.foundation.Blue200
import com.whatever.caro.core.designsystem.foundation.Blue300
import com.whatever.caro.core.designsystem.foundation.Blue400
import com.whatever.caro.core.designsystem.foundation.Blue500
import com.whatever.caro.core.designsystem.foundation.GradientTertiaryEnd
import com.whatever.caro.core.designsystem.foundation.GradientTertiaryStart
import com.whatever.caro.core.designsystem.foundation.Gray100
import com.whatever.caro.core.designsystem.foundation.Gray300
import com.whatever.caro.core.designsystem.foundation.Gray400
import com.whatever.caro.core.designsystem.foundation.Gray500
import com.whatever.caro.core.designsystem.foundation.Red200
import com.whatever.caro.core.designsystem.foundation.Red500
import com.whatever.caro.core.designsystem.foundation.White100
import com.whatever.caro.core.designsystem.foundation.Yellow200
import com.whatever.caro.core.designsystem.foundation.Yellow500

@Immutable
data class CaroColor(
    val text: TextColor,
    val icon: IconColor,
    val background: BackgroundColor,
    val surface: SurfaceColor,
    val border: BorderColor,
    val divider: DividerColor,
    val overlay: OverlayColor,
    val gradient: GradientColor,
) {
    @Immutable
    data class TextColor(
        val accent: Color = Blue500,
        val brand: Color = Blue500,
        val dangerous: Color = Red500,
        val disabled: Color = Gray300,
        val error: Color = Red500,
        val info: Color = Blue500,
        val inverse: Color = White100,
        val new: Color = Yellow500,
        val primary: Color = Black100,
        val rest: Color = Red500,
        val review: Color = Blue500,
        val secondary: Color = Gray500,
        val tertiary: Color = Gray300,
        val warning: Color = Yellow500,
    )

    @Immutable
    data class IconColor(
        val accent: Color = Blue500,
        val brand: Color = Blue500,
        val dangerous: Color = Red500,
        val disabled: Color = Gray300,
        val inverse: Color = White100,
        val primary: Color = Black100,
        val quaternary: Color = Gray400,
        val secondary: Color = Gray300,
        val tertiary: Color = Blue400,
        val warning: Color = Yellow500,
    )

    @Immutable
    data class BackgroundColor(
        val brand: Color = Blue500,
        val primary: Color = Background100,
    )

    @Immutable
    data class SurfaceColor(
        val accent: Color = Red500,
        val brand: Color = Blue500,
        val complete: Color = Gray100,
        val dangerous: Color = Red200,
        val disabled: Color = Blue200,
        val error: Color = Red200,
        val info: Color = Blue200,
        val inverse: Color = Black100,
        val new: Color = Yellow200,
        val primary: Color = White100,
        val progress: Color = Blue200,
        val ready: Color = Blue100,
        val rest: Color = Yellow200,
        val review: Color = Blue200,
        val secondary: Color = Blue100,
        val tertiary: Color = Blue200,
    )

    @Immutable
    data class BorderColor(
        val brand: Color = Blue500,
        val complete: Color = Gray300,
        val primary: Color = Blue100,
        val progress: Color = Blue300,
        val ready: Color = Blue200,
        val rest: Color = Yellow200,
        val secondary: Color = Gray300,
        val tertiary: Color = Blue300,
    )

    @Immutable
    data class DividerColor(
        val primary: Color = Blue300,
        val secondary: Color = Blue200,
    )

    @Immutable
    data class OverlayColor(
        val dim: Color = BlackAlpha60,
        val light: Color = BlackAlpha20,
    )

    @Immutable
    data class GradientColor(
        val tertiaryStart: Color = GradientTertiaryStart,
        val tertiaryEnd: Color = GradientTertiaryEnd,
    )

    companion object {
        fun defaultColor(): CaroColor =
            CaroColor(
                text = TextColor(),
                icon = IconColor(),
                background = BackgroundColor(),
                surface = SurfaceColor(),
                border = BorderColor(),
                divider = DividerColor(),
                overlay = OverlayColor(),
                gradient = GradientColor(),
            )
    }
}
