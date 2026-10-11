package com.whatever.caro.feature.importer.screenshot

import kotlin.test.Test

class ImportSavingPausedScreenshotIosTest {
    @Test
    fun savingPaused() {
        captureImportScreenshot(ImportSavingPausedScreenshotScenario.ID) { ImportSavingPausedScreenshotScenario.Render() }
    }
}
