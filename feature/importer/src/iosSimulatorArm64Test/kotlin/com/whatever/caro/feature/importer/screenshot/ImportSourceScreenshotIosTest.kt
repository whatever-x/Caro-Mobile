package com.whatever.caro.feature.importer.screenshot

import kotlin.test.Test

class ImportSourceScreenshotIosTest {
    @Test
    fun initial() {
        captureImportScreenshot(ImportSourceScreenshotScenario.INITIAL_ID) { ImportSourceScreenshotScenario.RenderInitial() }
    }

    @Test
    fun pasteFilled() {
        captureImportScreenshot(ImportSourceScreenshotScenario.PASTE_FILLED_ID) { ImportSourceScreenshotScenario.RenderPasteFilled() }
    }
}
