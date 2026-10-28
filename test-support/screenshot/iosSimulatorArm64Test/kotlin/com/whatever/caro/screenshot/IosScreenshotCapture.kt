package com.whatever.caro.screenshot

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.captureRoboImage
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults

internal fun iosScreenshotDefaultsDomain(): String =
    NSBundle.mainBundle.bundleIdentifier?.takeIf { it.isNotBlank() } ?: NSProcessInfo.processInfo.processName

internal fun <T> withIosScreenshotLocale(
    language: String,
    block: () -> T,
): T {
    val defaults = NSUserDefaults.standardUserDefaults
    val domain = iosScreenshotDefaultsDomain()
    return withRestoredSetting(
        // objectForKey includes the global fallback, which must not become an app override.
        read = { defaults.persistentDomainForName(domain)?.get("AppleLanguages") },
        write = { value ->
            if (value == null) defaults.removeObjectForKey("AppleLanguages") else defaults.setObject(value, forKey = "AppleLanguages")
        },
        replacement = listOf(language),
        block = block,
    )
}

@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
internal fun captureIosScreenshots(scenarios: () -> List<ScreenshotScenario>) {
    screenshotCaptures("ios", scenarios).forEach { capture ->
        withIosScreenshotLocale(capture.language) {
            val profile = capture.profile
            runSkikoComposeUiTest(
                size = Size(profile.widthPx.toFloat(), profile.heightPx.toFloat()),
                density = Density(profile.density, profile.fontScale),
            ) {
                mainClock.autoAdvance = false
                setContent { ScreenshotCanvas(capture) }
                mainClock.advanceTimeBy(1_000)
                waitForIdle()
                // Complete scroll/click work, then restore the paused capture clock.
                withRestoredSetting({ mainClock.autoAdvance }, { mainClock.autoAdvance = it }, true) {
                    capture.scenario.beforeCapture(this, capture.language)
                    waitForIdle()
                }
                val root = screenshotRoot(capture)
                assertScreenshotGeometry(capture, root.fetchSemanticsNode().size)
                root.captureRoboImage(this, filePath = capture.fileName)
                withRestoredSetting({ mainClock.autoAdvance }, { mainClock.autoAdvance = it }, true) {
                    capture.scenario.afterCapture(this, capture.language)
                    waitForIdle()
                }
            }
        }
    }
}
