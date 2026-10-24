package com.whatever.caro.feature.importer.screenshot

import androidx.compose.runtime.Composable
import com.whatever.caro.feature.importer.mvi.ImportDeckState
import com.whatever.caro.feature.importer.mvi.ImportDialog

internal object ImportErrorTooLargeScreenshotScenario {
    const val ID = "importer.error.too_large"

    @Composable
    fun Render() {
        ImportScreenshotScenario.Render(ImportDeckState(dialog = ImportDialog.FileTooLarge(extension = "apkg")))
    }
}
