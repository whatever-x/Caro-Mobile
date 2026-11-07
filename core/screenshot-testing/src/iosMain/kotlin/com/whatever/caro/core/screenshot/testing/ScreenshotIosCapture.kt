package com.whatever.caro.core.screenshot.testing

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import io.github.takahirom.roborazzi.captureRoboImage
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

@OptIn(ExperimentalTestApi::class, ExperimentalRoborazziApi::class)
fun captureIosScreenshots(scenarios: () -> List<ScreenshotScenario>) {
    for (capture in screenshotCaptures("ios", scenarios)) {
        val strings = prepareScreenshotStrings(capture, expectedIosResourceEnvironments())
        withIosScreenshotLocale(capture.language) {
            val profile = capture.profile
            runSkikoComposeUiTest(
                size = Size(profile.widthPx.toFloat(), profile.heightPx.toFloat()),
                density = Density(profile.density, profile.fontScale),
            ) {
                fun interaction(block: () -> Unit) {
                    val previous = mainClock.autoAdvance
                    try {
                        mainClock.autoAdvance = true
                        block()
                        waitForIdle()
                    } finally {
                        mainClock.autoAdvance = previous
                    }
                }
                mainClock.autoAdvance = false
                setContent { ScreenshotCanvas(capture) }
                mainClock.advanceTimeBy(1_000)
                waitForIdle()
                interaction { capture.scenario.beforeCapture(this, strings) }
                val component =
                    if (capture.scenario.target == ScreenshotTarget.DIALOG) {
                        onNodeWithTag(checkNotNull(capture.scenario.iosDialogContentTag))
                    } else {
                        onRoot()
                    }
                reportScreenshotGeometry(capture, component.fetchSemanticsNode().size)
                component.captureRoboImage(this, filePath = capture.fileName)
                interaction { capture.scenario.afterCapture(this, strings) }
            }
        }
    }
}

internal fun iosScreenshotDefaultsDomain(): String =
    NSBundle.mainBundle.bundleIdentifier?.takeIf { it.isNotBlank() } ?: NSProcessInfo.processInfo.processName

internal fun <T> withIosScreenshotLocale(
    language: String,
    block: () -> T,
): T {
    val defaults = NSUserDefaults.standardUserDefaults
    val domain = iosScreenshotDefaultsDomain()
    // Snapshot only the stored app key; an effective system fallback must remain a fallback.
    val previous = defaults.persistentDomainForName(domain)?.get("AppleLanguages")
    try {
        defaults.setObject(listOf(language), forKey = "AppleLanguages")
        return block()
    } finally {
        if (previous == null) defaults.removeObjectForKey("AppleLanguages") else defaults.setObject(previous, forKey = "AppleLanguages")
    }
}

// Prepare expected environments before capture locale setup, and restore only the stored app override.
private fun expectedIosResourceEnvironments(): Map<String, ResourceEnvironment> {
    val defaults = NSUserDefaults.standardUserDefaults
    val domain = iosScreenshotDefaultsDomain()
    val previous = defaults.persistentDomainForName(domain)?.get("AppleLanguages")
    try {
        return ScreenshotConfiguration.languages.associateWith { language ->
            defaults.setObject(listOf(language), forKey = "AppleLanguages")
            check(NSLocale.preferredLanguages.firstOrNull() == language) {
                "Expected resource language was not applied: $language"
            }
            getSystemResourceEnvironment()
        }
    } finally {
        if (previous == null) defaults.removeObjectForKey("AppleLanguages") else defaults.setObject(previous, forKey = "AppleLanguages")
    }
}
