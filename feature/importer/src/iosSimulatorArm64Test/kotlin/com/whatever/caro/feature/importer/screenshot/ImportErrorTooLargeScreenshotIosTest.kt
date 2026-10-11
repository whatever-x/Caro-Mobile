package com.whatever.caro.feature.importer.screenshot

import kotlin.test.Test

class ImportErrorTooLargeScreenshotIosTest {
    @Test
    fun tooLarge() {
        captureImportScreenshot(ImportErrorTooLargeScreenshotScenario.ID) { ImportErrorTooLargeScreenshotScenario.Render() }
    }
}
