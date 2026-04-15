package com.whatever.caro.core.designsystem.themes

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.whatever.caro.core.designsystem.foundation.Alpha20
import com.whatever.caro.core.designsystem.foundation.Alpha60
import com.whatever.caro.core.designsystem.foundation.Blue100
import com.whatever.caro.core.designsystem.foundation.Blue200
import com.whatever.caro.core.designsystem.foundation.Blue300
import com.whatever.caro.core.designsystem.foundation.Blue400
import com.whatever.caro.core.designsystem.foundation.Blue500
import com.whatever.caro.core.designsystem.foundation.Blue600
import com.whatever.caro.core.designsystem.foundation.Blue700
import com.whatever.caro.core.designsystem.foundation.Blue900
import com.whatever.caro.core.designsystem.foundation.Gray100
import com.whatever.caro.core.designsystem.foundation.Gray200
import com.whatever.caro.core.designsystem.foundation.Gray300
import com.whatever.caro.core.designsystem.foundation.Gray400
import com.whatever.caro.core.designsystem.foundation.Gray500
import com.whatever.caro.core.designsystem.foundation.Gray700
import com.whatever.caro.core.designsystem.foundation.Gray900
import com.whatever.caro.core.designsystem.foundation.Red100
import com.whatever.caro.core.designsystem.foundation.Red300
import com.whatever.caro.core.designsystem.foundation.Red500
import com.whatever.caro.core.designsystem.foundation.Red700
import com.whatever.caro.core.designsystem.foundation.White100
import com.whatever.caro.core.designsystem.foundation.Yellow100
import com.whatever.caro.core.designsystem.foundation.Yellow300
import com.whatever.caro.core.designsystem.foundation.Yellow700

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
) {
    @Immutable
    data class TextColor(
        val primary: Color = Gray900,
        val secondary: Color = Gray700,
        val tertiary: Color = Gray500,
        val disable: Color = Gray400,
        val inverse: Color = White100,
        val brand: Color = Blue500,
        val warning: Color = Yellow700,
        val error: Color = Red700,
        val info: Color = Blue600,
        val watermark: Color = Gray100,
        val accent: Color = Red500,
    )

    @Immutable
    data class IconColor(
        val primary: Color = Gray900,
        val secondary: Color = Gray700,
        val tertiary: Color = Gray500,
        val disable: Color = Gray400,
        val inverse: Color = White100,
        val brand: Color = Blue500,
        val warning: Color = Yellow700,
        val error: Color = Red700,
        val accent: Color = Red500,
    )

    @Immutable
    data class BackgroundColor(
        val primary: Color = Gray100,
        val secondary: Color = Blue500,
    )

    @Immutable
    data class SurfaceColor(
        val primary: Color = White100,
        val secondary: Color = Gray100,
        val tertiary: Color = Gray200,
        val inverse: Color = Gray900,
        val brand: Color = Blue500,
        val warning: Color = Yellow100,
        val error: Color = Red100,
        val disable: Color = Gray400,
        val info: Color = Blue100,
        val watermark: Color = Gray100,
        val accent: Color = Red500,
    )

    @Immutable
    data class BorderColor(
        val primary: Color = Gray300,
        val secondary: Color = Gray200,
        val tertiary: Color = Gray100,
        val disable: Color = Gray200,
        val brand: Color = Blue500,
        val warning: Color = Yellow300,
        val error: Color = Red300,
        val info: Color = Blue300,
    )

    @Immutable
    data class DividerColor(
        val primary: Color = Gray300,
        val secondary: Color = Gray200,
    )

    @Immutable
    data class OverlayColor(
        val dim: Color = Alpha60,
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
    )

    companion object {
        fun defaultColor() = CaroColor(
            text = TextColor(),
            icon = IconColor(),
            background = BackgroundColor(),
            surface = SurfaceColor(),
            border = BorderColor(),
            divider = DividerColor(),
            overlay = OverlayColor(),
            skeleton = SkeletonColor(),
            gradient = GradientColor(),
        )
    }
}