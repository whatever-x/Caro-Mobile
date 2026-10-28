package com.whatever.caro.screenshot

import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import com.github.takahirom.roborazzi.captureRoboImage
import org.robolectric.RuntimeEnvironment
import java.util.Locale

internal fun <T> withAndroidScreenshotLocale(
    language: String,
    block: () -> T,
): T {
    val previousLocale = Locale.getDefault()
    val previousLocales = LocaleList.getDefault()
    val previousQualifiers = RuntimeEnvironment.getQualifiers()
    try {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        LocaleList.setDefault(LocaleList(locale))
        RuntimeEnvironment.setQualifiers("+$language")
        return block()
    } finally {
        RuntimeEnvironment.setQualifiers(previousQualifiers)
        LocaleList.setDefault(previousLocales)
        Locale.setDefault(previousLocale)
    }
}

internal fun captureAndroidScreenshot(
    rule: AndroidComposeTestRule<*, ComponentActivity>,
    capture: ScreenshotCapture,
) {
    withAndroidScreenshotLocale(capture.language) {
        // The window is an explicit virtual Compose canvas; system bars are not captured.
        rule.runOnUiThread {
            rule.activity.window.setDecorFitsSystemWindows(false)
            rule.activity.window.setLayout(capture.profile.widthPx, capture.profile.heightPx)
        }
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val configuration = android.content.res.Configuration(LocalConfiguration.current)
            configuration.setLocales(LocaleList(Locale.forLanguageTag(capture.language)))
            configuration.fontScale = capture.profile.fontScale
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                ScreenshotCanvas(capture)
            }
        }
        // Advance a fixed amount rather than following infinite animation clocks.
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        // Scroll semantics need frames to finish before querying their updated bounds.
        withRestoredSetting({ rule.mainClock.autoAdvance }, { rule.mainClock.autoAdvance = it }, true) {
            capture.scenario.beforeCapture(rule, capture.language)
            rule.waitForIdle()
        }
        val root = rule.screenshotRoot(capture)
        val node = root.fetchSemanticsNode()
        val size = node.size
        assertScreenshotGeometry(capture, size)
        if (capture.scenario.isDialog) {
            // Capture the Dialog's content owner, bypassing both the all-window fallback
            // and Compose captureToImage's frame-commit wait under Robolectric.
            val view = checkNotNull(node.root as? ViewRootForTest) { "Dialog has no Android Compose owner" }.view
            check(view.isAttachedToWindow && view.width == size.width && view.height == size.height) {
                "${capture.fileName}: Dialog owner ${view.width}x${view.height}, attached=${view.isAttachedToWindow} != measured component $size"
            }
            view.captureRoboImage(capture.fileName)
        } else {
            root.captureRoboImage(capture.fileName)
        }
        withRestoredSetting({ rule.mainClock.autoAdvance }, { rule.mainClock.autoAdvance = it }, true) {
            capture.scenario.afterCapture(rule, capture.language)
            rule.waitForIdle()
        }
    }
}
