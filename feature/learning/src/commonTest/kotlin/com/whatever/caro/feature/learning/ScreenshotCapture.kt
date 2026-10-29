package com.whatever.caro.feature.learning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

internal data class ScreenshotProfile(
    val platform: String,
    val variant: String,
    val id: String,
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val fontScale: Float,
)

internal object ScreenshotConfiguration {
    val languages = listOf("ko", "en")
    val profiles =
        listOf(
            ScreenshotProfile("android", "default", "pixel10-pro", 1280, 2856, 3f, 1f),
            ScreenshotProfile("android", "small-large-font", "pixel10-pro-small-large-font", 960, 1440, 3f, 1.5f),
            ScreenshotProfile("ios", "default", "iphone18-pro", 1206, 2622, 3f, 1f),
            ScreenshotProfile("ios", "small-large-font", "iphone18-pro-small-large-font", 960, 1440, 3f, 1.5f),
        )
    val variants = mapOf("setting.notification.small.large-font" to "small-large-font")
    val contentSizedScenarios = setOf("setting.notification.permission")
}

internal class ScreenshotScenario(
    val id: String,
    val render: @Composable () -> Unit,
    val isDialog: Boolean = false,
    val beforeCapture: SemanticsNodeInteractionsProvider.(String) -> Unit = {},
    val afterCapture: SemanticsNodeInteractionsProvider.(String) -> Unit = {},
)

internal data class ScreenshotCapture(
    val scenario: ScreenshotScenario,
    val language: String,
    val profile: ScreenshotProfile,
) {
    var renderedDensity: Density? = null

    val fileName = "${scenario.id}__${language}__${profile.id}.png"

    override fun toString(): String = fileName
}

// A fresh scenario list per language keeps callback counters isolated.
internal fun screenshotCaptures(
    platform: String,
    scenarios: () -> List<ScreenshotScenario>,
): List<ScreenshotCapture> =
    ScreenshotConfiguration.languages.flatMap { language ->
        val fresh = scenarios()
        require(fresh.map { it.id }.distinct().size == fresh.size) { "Duplicate executable scenario ID" }
        fresh.map { scenario ->
            val variant = ScreenshotConfiguration.variants[scenario.id] ?: "default"
            val profile = ScreenshotConfiguration.profiles.single { it.platform == platform && it.variant == variant }
            ScreenshotCapture(scenario, language, profile)
        }
    }

@Composable
internal fun ScreenshotCanvas(capture: ScreenshotCapture) {
    val profile = capture.profile
    CompositionLocalProvider(LocalDensity provides Density(profile.density, profile.fontScale)) {
        val density = LocalDensity.current
        SideEffect { capture.renderedDensity = density }
        Box(Modifier.requiredSize((profile.widthPx / profile.density).dp, (profile.heightPx / profile.density).dp)) {
            capture.scenario.render()
        }
    }
}

internal fun assertScreenshotGeometry(
    capture: ScreenshotCapture,
    size: IntSize,
) {
    val profile = capture.profile
    if (capture.scenario.id in ScreenshotConfiguration.contentSizedScenarios) {
        check(size.width in 1..profile.widthPx && size.height in 1..profile.heightPx) {
            "${capture.fileName}: content root $size exceeds virtual canvas"
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
