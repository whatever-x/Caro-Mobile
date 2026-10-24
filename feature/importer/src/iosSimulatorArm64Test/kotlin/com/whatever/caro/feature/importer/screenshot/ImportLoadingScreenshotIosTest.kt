package com.whatever.caro.feature.importer.screenshot

import kotlin.test.Test

class ImportLoadingScreenshotIosTest {
    @Test
    fun parsingLoading() {
        captureImportLoadingScreenshot(ImportLoadingScreenshotScenario.ID) { ImportLoadingScreenshotScenario.Render() }
    }
}
