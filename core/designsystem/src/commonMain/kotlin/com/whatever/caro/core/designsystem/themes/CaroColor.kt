package com.whatever.caro.core.designsystem.themes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.whatever.caro.core.designsystem.foundation.Alpha20
import com.whatever.caro.core.designsystem.foundation.BlackAlpha60
import com.whatever.caro.core.designsystem.foundation.Blue100
import com.whatever.caro.core.designsystem.foundation.Blue200
import com.whatever.caro.core.designsystem.foundation.Blue300
import com.whatever.caro.core.designsystem.foundation.Blue400
import com.whatever.caro.core.designsystem.foundation.Blue500
import com.whatever.caro.core.designsystem.foundation.Blue600
import com.whatever.caro.core.designsystem.foundation.Blue700
import com.whatever.caro.core.designsystem.foundation.Blue900
import com.whatever.caro.core.designsystem.foundation.BorderComplete
import com.whatever.caro.core.designsystem.foundation.BorderProgress
import com.whatever.caro.core.designsystem.foundation.BorderReady
import com.whatever.caro.core.designsystem.foundation.ButtonFairDefault
import com.whatever.caro.core.designsystem.foundation.ButtonFairPressed
import com.whatever.caro.core.designsystem.foundation.ButtonHardPressed
import com.whatever.caro.core.designsystem.foundation.GradientTertiaryEnd
import com.whatever.caro.core.designsystem.foundation.GradientTertiaryStart
import com.whatever.caro.core.designsystem.foundation.Gray100
import com.whatever.caro.core.designsystem.foundation.Gray200
import com.whatever.caro.core.designsystem.foundation.Gray300
import com.whatever.caro.core.designsystem.foundation.Gray400
import com.whatever.caro.core.designsystem.foundation.Gray500
import com.whatever.caro.core.designsystem.foundation.Gray700
import com.whatever.caro.core.designsystem.foundation.Gray800
import com.whatever.caro.core.designsystem.foundation.Gray900
import com.whatever.caro.core.designsystem.foundation.IconDisabled
import com.whatever.caro.core.designsystem.foundation.Red100
import com.whatever.caro.core.designsystem.foundation.Red300
import com.whatever.caro.core.designsystem.foundation.Red500
import com.whatever.caro.core.designsystem.foundation.Red700
import com.whatever.caro.core.designsystem.foundation.Red800
import com.whatever.caro.core.designsystem.foundation.SurfaceComplete
import com.whatever.caro.core.designsystem.foundation.SurfaceProgress
import com.whatever.caro.core.designsystem.foundation.SurfaceReady
import com.whatever.caro.core.designsystem.foundation.TextComplete
import com.whatever.caro.core.designsystem.foundation.TextDisabled
import com.whatever.caro.core.designsystem.foundation.TextProgress
import com.whatever.caro.core.designsystem.foundation.TextReady
import com.whatever.caro.core.designsystem.foundation.White100
import com.whatever.caro.core.designsystem.foundation.Yellow100
import com.whatever.caro.core.designsystem.foundation.Yellow200
import com.whatever.caro.core.designsystem.foundation.Yellow300
import com.whatever.caro.core.designsystem.foundation.Yellow700
import com.whatever.caro.core.designsystem.foundation.Yellow800

@Immutable
data class CaroColor(
    val text: TextColor,
    val icon: IconColor,
    val background: BackgroundColor,
    val surface: SurfaceColor,
    val border: BorderColor,
    val divider: DividerColor,
    val overlay: OverlayColor,
    val skeleton: SkeletonColor,
    val gradient: GradientColor,
    val button: ButtonColor,
) {
    @Immutable
    data class TextColor(
        val primary: Color = Gray900,
        val secondary: Color = Gray700,
        val tertiary: Color = Gray500,
        val disable: Color = TextDisabled,
        val inverse: Color = White100,
        val brand: Color = Blue500,
        val warning: Color = Yellow800,
        val error: Color = Red700,
        val info: Color = Blue500,
        val ready: Color = TextReady,
        val progress: Color = TextProgress,
        val complete: Color = TextComplete,
        val watermark: Color = Gray100,
        val accent: Color = Red500,
        val rest: Color = Red800,
    )

    @Immutable
    data class IconColor(
        val primary: Color = Blue500,
        val secondary: Color = Gray700,
        val tertiary: Color = Blue400,
        val disable: Color = IconDisabled,
        val inverse: Color = White100,
        val brand: Color = Blue500,
        val warning: Color = Yellow700,
        val error: Color = Red700,
        val accent: Color = Red500,
    )

    @Immutable
    data class BackgroundColor(
        val primary: Color = Gray100,
        val brand: Color = Blue500,
    )

    @Immutable
    data class SurfaceColor(
        val primary: Color = White100,
        val secondary: Color = Gray100,
        val tertiary: Color = Blue100,
        val inverse: Color = Gray900,
        val brand: Color = Blue500,
        val warning: Color = Yellow100,
        val error: Color = Red100,
        val disabled: Color = Gray400,
        val info: Color = Blue100,
        val ready: Color = SurfaceReady,
        val progress: Color = SurfaceProgress,
        val complete: Color = SurfaceComplete,
        val accent: Color = Red500,
        val rest: Color = Yellow200,
    )

    @Immutable
    data class BorderColor(
        val primary: Color = SurfaceReady,
        val secondary: Color = Gray500,
        val tertiary: Color = Gray100,
        val disabled: Color = Gray200,
        val brand: Color = Blue500,
        val warning: Color = Yellow300,
        val error: Color = Red300,
        val info: Color = Blue300,
        val ready: Color = BorderReady,
        val progress: Color = BorderProgress,
        val complete: Color = BorderComplete,
        val rest: Color = Yellow200,
    )

    @Immutable
    data class DividerColor(
        val primary: Color = Gray300,
        val secondary: Color = Gray200,
    )

    @Immutable
    data class OverlayColor(
        val dim: Color = BlackAlpha60,
        val light: Color = Alpha20,
    )

    @Immutable
    data class SkeletonColor(
        val primary: Color = Gray200,
        val secondary: Color = Gray100,
        val inverse: Color = Alpha20,
    )

    @Immutable
    data class GradientColor(
        val primaryStart: Color = Blue900,
        val primaryEnd: Color = Blue700,
        val secondaryStart: Color = Blue200,
        val secondaryCenter: Color = Blue400,
        val secondaryEnd: Color = Blue600,
        val tertiaryStart: Color = GradientTertiaryStart,
        val tertiaryEnd: Color = GradientTertiaryEnd,
    )

    @Immutable
    data class ButtonColor(
        val surface: ButtonSurfaceDefaultColor = ButtonSurfaceDefaultColor(),
        val pressed: ButtonSurfacePressedColor = ButtonSurfacePressedColor(),
    ) {
        @Immutable
        data class ButtonSurfaceDefaultColor(
            val floating: Color = Gray800,
            val easy: Color = BorderProgress,
            val fair: Color = ButtonFairDefault,
            val hard: Color = Red300,
        )

        @Immutable
        data class ButtonSurfacePressedColor(
            val easy: Color = Blue400,
            val fair: Color = ButtonFairPressed,
            val hard: Color = ButtonHardPressed,
        )
    }

    companion object {
        fun defaultColor(): CaroColor {
            val textColor = TextColor()

            return CaroColor(
                text = textColor,
                icon = IconColor(),
                background = BackgroundColor(),
                surface = SurfaceColor(),
                border = BorderColor(),
                divider = DividerColor(),
                overlay = OverlayColor(),
                skeleton = SkeletonColor(),
                gradient = GradientColor(),
                button = ButtonColor(),
            )
        }
    }
}
