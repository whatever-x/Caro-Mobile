package com.whatever.caro.core.designsystem.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import com.whatever.caro.core.designsystem.foundation.Pretendard
import com.whatever.caro.core.designsystem.foundation.Roboto

val LocalCaroTypography = compositionLocalOf<CaroTypography> { error("Typography Error") }

val LocalCaroShape = compositionLocalOf<CaroShape> { error("Shape Error") }

val LocalCaroSpacing = compositionLocalOf<CaroSpacing> { error("Spacing Error") }

val LocalCaroColor = compositionLocalOf<CaroColor> { error("Color Error") }

@Composable
fun CaroTheme(
    color: CaroColor = CaroColor.defaultColor(),
    typography: CaroTypography = CaroTypography.defaultTypography(
        pretendard = Pretendard(),
        roboto = Roboto(),
    ),
    shape: CaroShape = CaroShape.defaultShape(),
    spacing: CaroSpacing = CaroSpacing.defaultSpacing(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalCaroColor provides color,
        LocalCaroTypography provides typography,
        LocalCaroShape provides shape,
        LocalCaroSpacing provides spacing,
    ) {
        content.invoke()
    }
}

object CaroTheme {
    val color: CaroColor
        @Composable
        @ReadOnlyComposable
        get() = LocalCaroColor.current

    val typography: CaroTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalCaroTypography.current

    val shape: CaroShape
        @Composable
        @ReadOnlyComposable
        get() = LocalCaroShape.current

    val spacing: CaroSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalCaroSpacing.current
}