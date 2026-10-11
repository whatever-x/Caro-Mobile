package com.whatever.caro.feature.importer.screenshot

import kotlin.test.Test

class ImportSaveScreenshotIosTest {
    @Test
    fun deckInfoPrefilled() {
        captureImportScreenshot(
            ImportSaveScreenshotScenario.DECK_INFO_PREFILLED_ID,
        ) { ImportSaveScreenshotScenario.RenderDeckInfoPrefilled() }
    }

    @Test
    fun savingProgress() {
        captureImportScreenshot(ImportSaveScreenshotScenario.SAVING_PROGRESS_ID) { ImportSaveScreenshotScenario.RenderSavingProgress() }
    }
}
