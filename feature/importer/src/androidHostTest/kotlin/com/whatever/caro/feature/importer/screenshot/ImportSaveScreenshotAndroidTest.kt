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
class ImportSaveScreenshotAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deckInfoPrefilled() {
        composeRule.captureImportScreenshot(ImportSaveScreenshotScenario.DECK_INFO_PREFILLED_ID) {
            ImportSaveScreenshotScenario.RenderDeckInfoPrefilled()
        }
    }

    @Test
    fun savingProgress() {
        composeRule.captureImportScreenshot(
            ImportSaveScreenshotScenario.SAVING_PROGRESS_ID,
        ) { ImportSaveScreenshotScenario.RenderSavingProgress() }
    }
}
