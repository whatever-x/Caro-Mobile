package com.whatever.caro.core.screenshot.testing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
internal fun ScreenshotCanvas(capture: ScreenshotCapture) {
    val profile = capture.profile
    CompositionLocalProvider(LocalDensity provides Density(profile.density, profile.fontScale)) {
        val density = LocalDensity.current
        SideEffect { capture.renderedDensity = density }
        Box(Modifier.requiredSize((profile.widthPx / profile.density).dp, (profile.heightPx / profile.density).dp)) {
            if (profile.platform == "ios" && capture.scenario.target == ScreenshotTarget.DIALOG) {
                checkNotNull(capture.scenario.iosDialogContent) { "${capture.fileName}: missing isolated iOS dialog content" }.invoke()
            } else {
                capture.scenario.render()
            }
        }
    }
}

internal fun assertScreenshotGeometry(
    capture: ScreenshotCapture,
    size: IntSize,
) {
    val profile = capture.profile
    if (capture.scenario.target == ScreenshotTarget.DIALOG) {
        check(
            size.width in 1..profile.widthPx && size.height in 1..profile.heightPx &&
                size != IntSize(profile.widthPx, profile.heightPx),
        ) {
            "${capture.fileName}: dialog content root $size is empty, full-canvas, or exceeds virtual canvas"
        }
    } else {
        check(size == IntSize(profile.widthPx, profile.heightPx)) {
            "${capture.fileName}: measured root $size != ${profile.widthPx}x${profile.heightPx}"
        }
    }
}

internal fun reportScreenshotGeometry(
    capture: ScreenshotCapture,
    size: IntSize,
) {
    assertScreenshotGeometry(capture, size)
    val density = checkNotNull(capture.renderedDensity) { "${capture.fileName}: no composed density observation" }
    check(density.density == capture.profile.density && density.fontScale == capture.profile.fontScale) {
        "${capture.fileName}: composed density/fontScale differs from capture profile"
    }
    println(
        "SCREENSHOT_GEOMETRY ${capture.fileName} root=${size.width}x${size.height}px " +
            "density=${density.density} fontScale=${density.fontScale}",
    )
}
