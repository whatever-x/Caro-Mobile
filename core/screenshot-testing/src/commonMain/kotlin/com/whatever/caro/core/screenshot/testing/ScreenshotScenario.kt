package com.whatever.caro.core.screenshot.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.unit.Density

private val screenshotIdPattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")
private val reservedScreenshotSuffixes = listOf("_actual", "_compare", "_before")

/** The selected component also determines its geometry policy. */
enum class ScreenshotTarget {
    SCREEN,
    DIALOG,
}

class ScreenshotScenario(
    val id: String,
    val render: @Composable () -> Unit,
    val expectedStrings: List<ScreenshotString>,
    val target: ScreenshotTarget = ScreenshotTarget.SCREEN,
    val variant: ScreenshotVariant = ScreenshotVariant.DEFAULT,
    val iosDialogContent: (@Composable () -> Unit)? = null,
    val iosDialogContentTag: String? = null,
    val beforeCapture: SemanticsNodeInteractionsProvider.(ScreenshotStrings) -> Unit = {},
    val afterCapture: SemanticsNodeInteractionsProvider.(ScreenshotStrings) -> Unit = {},
)

data class ScreenshotCapture(
    val scenario: ScreenshotScenario,
    val language: String,
    val profile: ScreenshotProfile,
) {
    internal var renderedDensity: Density? = null

    val fileName = "${scenario.id}__${language}__${profile.id}.png"

    override fun toString(): String = fileName
}

// A fresh scenario list per language keeps callback counters isolated.
fun screenshotCaptures(
    platform: String,
    scenarios: () -> List<ScreenshotScenario>,
): List<ScreenshotCapture> =
    ScreenshotConfiguration.languages.flatMap { language ->
        val fresh = scenarios()
        require(fresh.distinctBy { it.id }.size == fresh.size) { "Duplicate executable scenario ID" }
        fresh.map { scenario ->
            require(
                scenario.id.matches(screenshotIdPattern) &&
                    "__" !in scenario.id && reservedScreenshotSuffixes.none { scenario.id.endsWith(it) },
            ) {
                "Invalid screenshot scenario ID: ${scenario.id}"
            }
            val profile = ScreenshotConfiguration.profiles.single { it.platform == platform && it.variant == scenario.variant }
            ScreenshotCapture(scenario, language, profile)
        }
    }
