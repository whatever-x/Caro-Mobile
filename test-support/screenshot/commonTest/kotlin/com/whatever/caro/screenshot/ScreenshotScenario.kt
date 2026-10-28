package com.whatever.caro.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onRoot
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

/** A new instance per capture keeps interaction counters independent across locales. */
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
    val fileName: String = "${scenario.id}__${language}__${profile.id}.png"

    override fun toString(): String = fileName
}

internal fun SemanticsNodeInteractionsProvider.screenshotRoot(capture: ScreenshotCapture): SemanticsNodeInteraction =
    if (capture.scenario.isDialog) {
        onNode(isDialog())
    } else {
        onRoot()
    }

internal fun screenshotCaptures(
    platform: String,
    scenarios: () -> List<ScreenshotScenario>,
): List<ScreenshotCapture> =
    ScreenshotConfiguration.languages.flatMap { language ->
        val fresh = scenarios()
        require(fresh.map { it.id }.distinct().size == fresh.size) { "Duplicate executable scenario ID" }
        fresh.map { scenario ->
            val variant = ScreenshotConfiguration.variants[scenario.id] ?: "default"
            ScreenshotCapture(
                scenario,
                language,
                ScreenshotConfiguration.profiles.single { it.platform == platform && it.variant == variant },
            )
        }
    }

@Composable
internal fun ScreenshotCanvas(capture: ScreenshotCapture) {
    val profile = capture.profile
    CompositionLocalProvider(LocalDensity provides Density(profile.density, profile.fontScale)) {
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
            "${capture.fileName}: content root $size exceeds virtual canvas ${profile.widthPx}x${profile.heightPx}"
        }
    } else {
        check(size == IntSize(profile.widthPx, profile.heightPx)) {
            "${capture.fileName}: measured Compose root $size != virtual canvas ${profile.widthPx}x${profile.heightPx}"
        }
    }
    println(
        "SCREENSHOT_GEOMETRY ${capture.fileName} root=${size.width}x${size.height}px density=${profile.density} fontScale=${profile.fontScale}",
    )
}

/** Restore process-wide settings even when rendering or an assertion throws. */
internal inline fun <T, R> withRestoredSetting(
    read: () -> T,
    write: (T) -> Unit,
    replacement: T,
    block: () -> R,
): R {
    val previous = read()
    try {
        write(replacement)
        return block()
    } finally {
        write(previous)
    }
}
