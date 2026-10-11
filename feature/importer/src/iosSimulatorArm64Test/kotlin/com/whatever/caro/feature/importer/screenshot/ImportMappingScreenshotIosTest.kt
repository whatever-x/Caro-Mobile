package com.whatever.caro.feature.importer.screenshot

import kotlin.test.Test

class ImportMappingScreenshotIosTest {
    @Test
    fun default() {
        captureImportScreenshot(ImportMappingScreenshotScenario.DEFAULT_ID) { ImportMappingScreenshotScenario.RenderDefault() }
    }

    @Test
    fun cloze() {
        captureImportScreenshot(ImportMappingScreenshotScenario.CLOZE_ID) { ImportMappingScreenshotScenario.RenderCloze() }
    }
}
