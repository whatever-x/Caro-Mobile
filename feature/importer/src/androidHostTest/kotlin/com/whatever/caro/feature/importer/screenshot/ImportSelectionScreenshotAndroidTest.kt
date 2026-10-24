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
class ImportSelectionScreenshotAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun subdeckOverLimit() {
        composeRule.captureImportScreenshot(ImportSelectionScreenshotScenario.SUBDECK_OVER_LIMIT_ID) {
            ImportSelectionScreenshotScenario.RenderSubdeckOverLimit()
        }
    }

    @Test
    fun sheetSelect() {
        composeRule.captureImportScreenshot(ImportSelectionScreenshotScenario.SHEET_SELECT_ID) {
            ImportSelectionScreenshotScenario.RenderSheetSelect()
        }
    }

    @Test
    fun textCheckDefault() {
        composeRule.captureImportScreenshot(ImportSelectionScreenshotScenario.TEXT_CHECK_DEFAULT_ID) {
            ImportSelectionScreenshotScenario.RenderTextCheckDefault()
        }
    }
}
