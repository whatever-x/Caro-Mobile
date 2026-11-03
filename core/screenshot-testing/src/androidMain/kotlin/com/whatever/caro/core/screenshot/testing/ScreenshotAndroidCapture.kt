package com.whatever.caro.core.screenshot.testing

import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.window.DialogWindowProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import com.github.takahirom.roborazzi.captureRoboImage
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import org.hamcrest.core.IsSame.sameInstance
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment
import java.util.Locale

fun screenshotAndroidRule(
    capture: ScreenshotCapture,
    composeRule: AndroidComposeTestRule<*, ComponentActivity>,
): TestRule =
    RuleChain
        .outerRule(
            TestRule { base, _ ->
                object : Statement() {
                    override fun evaluate() = withAndroidScreenshotLocale(capture.language, capture.profile) { base.evaluate() }
                }
            },
        ).around(composeRule)

fun captureAndroidScreenshot(
    composeRule: AndroidComposeTestRule<*, ComponentActivity>,
    capture: ScreenshotCapture,
) {
    val strings = prepareScreenshotStrings(capture, expectedAndroidResourceEnvironments())
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
    interaction(composeRule) { capture.scenario.beforeCapture(composeRule, strings) }
    val component = if (capture.scenario.target == ScreenshotTarget.DIALOG) composeRule.onNode(isDialog()) else composeRule.onRoot()
    val node = component.fetchSemanticsNode()
    val size = node.size
    reportScreenshotGeometry(capture, size)
    if (capture.scenario.target == ScreenshotTarget.DIALOG) {
        dialogScreenshotView(composeRule, capture, node).captureRoboImage(capture.fileName)
    } else {
        component.captureRoboImage(capture.fileName)
    }
    interaction(composeRule) { capture.scenario.afterCapture(composeRule, strings) }
}

// Bind Espresso to the Window that owns the selected semantics node.
// Roborazzi's attached View overload uses onView without an explicit root.
internal fun dialogScreenshotView(
    composeRule: AndroidComposeTestRule<*, ComponentActivity>,
    capture: ScreenshotCapture,
    node: SemanticsNode,
): ViewInteraction {
    val view = checkNotNull(node.root as? ViewRootForTest).view
    val size = node.size
    val decor =
        composeRule.runOnIdle {
            val windowProvider =
                generateSequence(view.parent) { it.parent }
                    .filterIsInstance<DialogWindowProvider>()
                    .single()
            val decorView = windowProvider.window.decorView
            check(view.isAttachedToWindow && view.width == size.width && view.height == size.height) {
                "${capture.fileName}: Dialog owner ${view.width}x${view.height} != measured component $size"
            }
            check(view.rootView === decorView && decorView !== composeRule.activity.window.decorView) {
                "${capture.fileName}: selected owner must belong to the Dialog Window"
            }
            decorView
        }
    return onView(sameInstance(view)).inRoot(withDecorView(sameInstance(decor)))
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
    profile: ScreenshotProfile = ScreenshotConfiguration.profiles.single { it.platform == "android" && it.variant == ScreenshotVariant.DEFAULT },
    block: () -> T,
): T {
    val previousLocale = Locale.getDefault()
    val previousLocales = LocaleList.getDefault()
    val previousQualifiers = RuntimeEnvironment.getQualifiers()
    try {
        // Integer host bounds enclose the exact virtual window; profile values stay in the shared Kotlin configuration.
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

// Independent from withAndroidScreenshotLocale: only prepare expected resource environments here.
private fun expectedAndroidResourceEnvironments(): Map<String, ResourceEnvironment> {
    val previousLocale = Locale.getDefault()
    val previousLocales = LocaleList.getDefault()
    try {
        return ScreenshotConfiguration.languages.associateWith { language ->
            val locale = Locale.forLanguageTag(language)
            Locale.setDefault(locale)
            LocaleList.setDefault(LocaleList(locale))
            check(Locale.getDefault().language == language) { "Expected resource language was not applied: $language" }
            getSystemResourceEnvironment()
        }
    } finally {
        LocaleList.setDefault(previousLocales)
        Locale.setDefault(previousLocale)
    }
}
