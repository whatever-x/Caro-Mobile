package com.whatever.caro.feature.importer.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = IMPORT_SCREENSHOT_QUALIFIERS)
class ImportSourceScreenshotAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initial() {
        composeRule.captureImportScreenshot(ImportSourceScreenshotScenario.INITIAL_ID) { ImportSourceScreenshotScenario.RenderInitial() }
    }

    @Test
    fun pasteFilled() {
        composeRule.captureImportScreenshot(
            ImportSourceScreenshotScenario.PASTE_FILLED_ID,
        ) { ImportSourceScreenshotScenario.RenderPasteFilled() }
    }
}
