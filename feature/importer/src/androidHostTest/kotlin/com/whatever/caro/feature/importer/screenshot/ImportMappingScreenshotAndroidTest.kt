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
class ImportMappingScreenshotAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun default() {
        composeRule.captureImportScreenshot(ImportMappingScreenshotScenario.DEFAULT_ID) { ImportMappingScreenshotScenario.RenderDefault() }
    }

    @Test
    fun cloze() {
        composeRule.captureImportScreenshot(ImportMappingScreenshotScenario.CLOZE_ID) { ImportMappingScreenshotScenario.RenderCloze() }
    }
}
