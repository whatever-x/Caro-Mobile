package com.whatever.caro.feature.importer.screenshot

import kotlin.test.Test

class ImportSelectionScreenshotIosTest {
    @Test
    fun subdeckOverLimit() {
        captureImportScreenshot(ImportSelectionScreenshotScenario.SUBDECK_OVER_LIMIT_ID) {
            ImportSelectionScreenshotScenario.RenderSubdeckOverLimit()
        }
    }

    @Test
    fun sheetSelect() {
        captureImportScreenshot(ImportSelectionScreenshotScenario.SHEET_SELECT_ID) { ImportSelectionScreenshotScenario.RenderSheetSelect() }
    }

    @Test
    fun textCheckDefault() {
        captureImportScreenshot(ImportSelectionScreenshotScenario.TEXT_CHECK_DEFAULT_ID) {
            ImportSelectionScreenshotScenario.RenderTextCheckDefault()
        }
    }
}
