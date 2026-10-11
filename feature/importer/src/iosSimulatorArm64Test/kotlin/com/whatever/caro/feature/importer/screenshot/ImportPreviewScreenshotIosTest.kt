package com.whatever.caro.feature.importer.screenshot

import kotlin.test.Test

class ImportPreviewScreenshotIosTest {
    @Test
    fun flags() {
        captureImportScreenshot(ImportPreviewScreenshotScenario.FLAGS_ID) { ImportPreviewScreenshotScenario.RenderFlags() }
    }

    @Test
    fun filtered() {
        captureImportScreenshot(ImportPreviewScreenshotScenario.FILTERED_ID) { ImportPreviewScreenshotScenario.RenderFiltered() }
    }
}
