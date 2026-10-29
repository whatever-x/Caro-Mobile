package com.whatever.caro.feature.profile

import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment
import java.util.Locale

internal fun screenshotAndroidRule(
    capture: ScreenshotCapture,
    composeRule: AndroidComposeTestRule<*, ComponentActivity>,
): TestRule =
    RuleChain
        .outerRule(
            TestRule { base, _ ->
                object : Statement() {
                    override fun evaluate() = withAndroidScreenshotLocale(capture.language) { base.evaluate() }
                }
            },
        ).around(composeRule)

internal fun captureAndroidScreenshot(
    composeRule: AndroidComposeTestRule<*, ComponentActivity>,
    capture: ScreenshotCapture,
) {
    composeRule.runOnUiThread {
        composeRule.activity.window.setDecorFitsSystemWindows(false)
        composeRule.activity.window.setLayout(capture.profile.widthPx, capture.profile.heightPx)
    }
    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
        val configuration = android.content.res.Configuration(LocalConfiguration.current)
        configuration.setLocales(LocaleList(Locale.forLanguageTag(capture.language)))
        configuration.fontScale = capture.profile.fontScale
        CompositionLocalProvider(LocalConfiguration provides configuration) { ScreenshotCanvas(capture) }
    }
    composeRule.mainClock.advanceTimeBy(1_000)
    composeRule.waitForIdle()
    interaction(composeRule) { capture.scenario.beforeCapture(composeRule, capture.language) }
    val component = if (capture.scenario.isDialog) composeRule.onNode(isDialog()) else composeRule.onRoot()
    val node = component.fetchSemanticsNode()
    val size = node.size
    reportScreenshotGeometry(capture, size)
    if (capture.scenario.isDialog) {
        // Public View capture avoids Roborazzi's all-window semantics fallback and Compose's redraw wait.
        val view = checkNotNull(node.root as? ViewRootForTest).view
        check(view.isAttachedToWindow && view.width == size.width && view.height == size.height) {
            "${capture.fileName}: Dialog owner ${view.width}x${view.height} != measured component $size"
        }
        view.captureRoboImage(capture.fileName)
    } else {
        component.captureRoboImage(capture.fileName)
    }
    interaction(composeRule) { capture.scenario.afterCapture(composeRule, capture.language) }
}

private fun interaction(
    composeRule: AndroidComposeTestRule<*, ComponentActivity>,
    block: () -> Unit,
) {
    val previous = composeRule.mainClock.autoAdvance
    try {
        composeRule.mainClock.autoAdvance = true
        block()
        composeRule.waitForIdle()
    } finally {
        composeRule.mainClock.autoAdvance = previous
    }
}

internal fun <T> withAndroidScreenshotLocale(
    language: String,
    block: () -> T,
): T {
    val previousLocale = Locale.getDefault()
    val previousLocales = LocaleList.getDefault()
    val previousQualifiers = RuntimeEnvironment.getQualifiers()
    val profile = ScreenshotConfiguration.profiles.single { it.platform == "android" && it.variant == "default" }
    try {
        // Integer host bounds enclose the exact virtual window; profile values stay in this feature's Kotlin configuration.
        val width = kotlin.math.ceil(profile.widthPx / profile.density.toDouble()).toInt()
        val height = kotlin.math.ceil(profile.heightPx / profile.density.toDouble()).toInt()
        val densityDpi = (profile.density * 160).toInt()
        RuntimeEnvironment.setQualifiers("$language-w${width}dp-h${height}dp-${densityDpi}dpi")
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        LocaleList.setDefault(LocaleList(locale))
        return block()
    } finally {
        RuntimeEnvironment.setQualifiers(previousQualifiers)
        LocaleList.setDefault(previousLocales)
        Locale.setDefault(previousLocale)
    }
}
